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
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer
import zio.console.putStrLn

import java.sql.SQLException
import java.util.Properties
import javax.sql.DataSource

object ProtoQuillDemo extends zio.App:

  def propsFromConfigWithPrefix(source: Config, prefix: String): Properties = {
    import scala.jdk.CollectionConverters.*
    val props = new Properties()
    source.entrySet.asScala.filter(_.getKey.startsWith(s"$prefix.")).foreach(entry =>
      props.put(entry.getKey.substring(prefix.length + 1), entry.getValue.unwrapped)
    )
    props
  } 

  val dataSourceLayer: ULayer[Has[DataSource]] = ZLayer.fromEffectMany(
    Task.effect {
      val source = ConfigFactory.load
      val config = new HikariConfig(propsFromConfigWithPrefix(source, "quill"))
      new HikariDataSource(config).asInstanceOf[DataSource]
    }.map(ds => Has(ds))
  ).orDie

  val dbMigrationLayer: ZLayer[Has[DataSource], Nothing, Has[Unit]] = ZLayer.fromService(ds =>
    Flyway
      .configure()
      .dataSource(ds)
      .locations("classpath:db/migration")
      .load()
      .migrate()
  )

  val runtimeDependencies: ULayer[Has[DataSource]] = dataSourceLayer >+> dbMigrationLayer

  case class Person(id: Int, firstName: String, lastName: String, age: Int)

  object QuillContext extends PostgresZioJdbcContext(SnakeCase)

  import QuillContext.*
  import io.getquill.*

  object Queries:
    val personOlderThan = quote { (age: Int) =>
      query[Person].filter(p => p.age > age)
    }
    val personNamed = quote { (name: String) =>
      query[Person].filter(p => p.firstName == name)
    }
    val allPersons = quote {
      query[Person]
    }

  val composed: IO[SQLException, List[Person]] =
    QuillContext
      .run(Queries.personNamed(lift("Jane")))
      .provideLayer(runtimeDependencies)

  def program =
    for {
      res <- composed
      _ <- putStrLn(s"$res")
    } yield ()

  override def run(args: List[String]) = program.exitCode
