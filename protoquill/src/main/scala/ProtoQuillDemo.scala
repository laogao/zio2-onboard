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

  val dataSourceLayer: ULayer[Has[DataSource]] =  ZLayer.fromEffectMany(
    Task.effect {
      val source = ConfigFactory.load
      val props = new Properties()
      props.setProperty("dataSourceClassName", source.getString("hikari.dataSourceClassName"));
      props.setProperty("dataSource.serverName", source.getString("hikari.dataSource.serverName"));
      props.setProperty("dataSource.portNumber", source.getString("hikari.dataSource.portNumber"));
      props.setProperty("dataSource.user", source.getString("hikari.dataSource.user"));
      props.setProperty("dataSource.password", source.getString("hikari.dataSource.password"));
      props.setProperty("dataSource.databaseName", source.getString("hikari.dataSource.databaseName"));
      val config = new HikariConfig(props)
      new HikariDataSource(config).asInstanceOf[DataSource]
    }.map(ds => Has(ds))
  ).orDie

  val dbMigrationLayer: ZLayer[Has[DataSource], Nothing, Has[DataSource]] = ZLayer.fromService(ds =>
    Flyway
      .configure()
      .dataSource(ds)
      .locations("classpath:db/migration") // this is the default
      .load()
      .migrate()
    ds
  )

  case class Person(id: Int, firstName: String, lastName: String, age: Int)

  object QuillContext extends PostgresZioJdbcContext(SnakeCase):
    val dataSourceLayer: ULayer[Has[DataSource]] =
      DataSourceLayer.fromPrefix("quill").orDie

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
      .provideLayer(dataSourceLayer >+> dbMigrationLayer)

  def program =
    for {
      res <- composed
      _ <- putStrLn(s"$res")
    } yield ()

  override def run(args: List[String]) = program.exitCode
