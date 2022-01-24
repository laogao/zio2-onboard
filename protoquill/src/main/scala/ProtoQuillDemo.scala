import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.getquill.EntityQuery
import io.getquill.PostgresZioJdbcContext
import io.getquill.Quoted
import io.getquill.SnakeCase
import io.getquill.context.ZioJdbc.DataSourceLayer
import org.flywaydb.core.Flyway
import zio.ExitCode
import zio.Has
import zio.IO
import zio.RIO
import zio.Task
import zio.UIO
import zio.ULayer
import zio.ZIO
import zio.ZLayer
import zio.console.putStrLn

import java.sql.SQLException
import java.util.Properties
import javax.sql.DataSource
import caliban.schema.Schema

object ProtoQuillDemo extends zio.App:

  def propsFromConfigWithPrefix(source: Config, prefix: String): Properties = {
    import scala.jdk.CollectionConverters.*
    val props = new Properties()
    source.entrySet.asScala.filter(_.getKey.startsWith(s"$prefix.")).foreach(entry =>
      props.put(entry.getKey.substring(prefix.length + 1), entry.getValue.unwrapped)
    )
    props
  } 

  trait DataSourceProvider {
    def getDataSource: DataSource
  }

  object DataSourceProvider {
    val fallback = new DataSourceProvider {
      lazy val ds = {
        println("creating datasource...")
        val source = ConfigFactory.load
        val config = new HikariConfig(propsFromConfigWithPrefix(source, "quill"))
        new HikariDataSource(config).asInstanceOf[DataSource]
      }
      def getDataSource = ds
    }
  }

  val dataSourceProviderLayer: ULayer[Has[DataSourceProvider]] = ZLayer.fromEffectMany(
    Task.effect(DataSourceProvider.fallback).map(dsp => Has(dsp))
  ).orDie

  val dbMigrationLayer: ZLayer[Has[DataSourceProvider], Nothing, Has[Unit]] = ZLayer.fromService(dsp =>
    Flyway
      .configure()
      .dataSource(dsp.getDataSource)
      .locations("classpath:db/migration")
      .load()
      .migrate()
  )

  val dataSourceLayer: ZLayer[Has[DataSourceProvider], Nothing, Has[DataSource]] = ZLayer.fromService(dsp => dsp.getDataSource)

  val runtimeDependencies: ULayer[Has[DataSourceProvider]] = dataSourceProviderLayer >+> dbMigrationLayer

  case class Person(id: Int, firstName: String, lastName: String, age: Int)

  object QuillContext extends PostgresZioJdbcContext(SnakeCase)

  import QuillContext.*
  import io.getquill.*

  object Queries:
    inline def allPersons = query[Person]
    inline def personNamed = (name: String) => query[Person].filter(p => p.firstName == name)
    inline def personOlderThan = (age: Int) => query[Person].filter(p => p.age > age)

  def allPersonsPrepared = 
    QuillContext.run(Queries.allPersons)

  def personNamedPrepared(name: String) = 
    QuillContext.run(Queries.personNamed(lift(name)))

  def personOlderThanPrepared(age: Int) = 
    QuillContext.run(Queries.personOlderThan(lift(age)))

  def executeQueries =
    for {
      all <- allPersonsPrepared
      james <- personNamedPrepared("James")
      adults <- personOlderThanPrepared(16)
    } yield (all, james, adults)
  
  import caliban.GraphQL.graphQL
  import caliban.RootResolver

  object GraphQL {
    case class Queries(
      allPersons: RIO[Has[DataSourceProvider], List[Person]]
    )
    val queryResolver = Queries(
      allPersons = allPersonsPrepared.provideLayer(dataSourceLayer).orDie
    )
    implicit val queriesSchema: Schema[Has[DataSourceProvider], Queries] = Schema.gen
    val api = graphQL(RootResolver(queryResolver))
  }

  import zhttp.http.*
  import zhttp.service.Server
  import caliban.ZHttpAdapter

  val graphQLServer = 
    for {
      has <- ZIO.environment
      _ <- putStrLn(s"Has map were: $has")
      ds <- ZIO.service[DataSourceProvider]
      _ <- putStrLn(s"DataSource found: $ds")
      _ <- putStrLn(s"DataSource#getConnection: ${ds.getDataSource.getConnection}")
      interpreter <- GraphQL.api.interpreter
      out <- interpreter.execute("""{allPersons{firstName, age}}""")
      _ <- putStrLn(s"GraphQL interpreter built. Sample query result: $out.")
      _ <- putStrLn(s"Starting HTTP server @ localhost:8088...")
      _ <- Server.start(
          8088,
          Http.route {
            case _ -> Root / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
            case _ -> Root / "ws" / "graphql"  => ZHttpAdapter.makeWebSocketService(interpreter)
          }
        ).forever
    } yield ()

  override def run(args: List[String]) = graphQLServer.provideCustomLayer(runtimeDependencies).exitCode
