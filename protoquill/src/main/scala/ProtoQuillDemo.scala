import com.typesafe.config.ConfigFactory
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

import java.sql.SQLException
import javax.sql.DataSource

object ProtoQuillDemo extends zio.App:

  val dbMigration = Task {
    val config = ConfigFactory.load
    val flyway = Flyway
      .configure()
      .dataSource(
        config.getString("quill.dataSource.url"),
        config.getString("quill.dataSource.user"),
        config.getString("quill.dataSource.password")
      )
      .locations("classpath:db/migration") // this is the default
      .load()
    flyway.migrate()
  }

  case class Person(id: Int, firstName: String, lastName: String, age: Int)

  val quillExample = Task {
    import io.getquill.*
    val ctx = new PostgresJdbcContext(SnakeCase, "quill")
    import ctx.*
    val named = "Joe"
    inline def somePeople = quote {
      query[Person].filter(p => p.firstName == lift(named))
    }
    val people: List[Person] = ctx.run(somePeople)
    println("[CONVENTIONAL QUERY] " + people)
  }

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
      .run(Queries.personNamed(lift("Joe")))
      .provideLayer(HikariCPHelper.hikariDataSourceLayer)

  def program =
    for {
      _ <- dbMigration
      _ <- quillExample
      res <- composed
      _ <- zio.console.putStrLn("[ZIO-COMPOSED QUERY] " + res)
    } yield ()

  override def run(args: List[String]) = program.exitCode
