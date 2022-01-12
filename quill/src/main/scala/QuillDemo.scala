import com.typesafe.config.ConfigFactory
import zio.{ExitCode, Task, IO}
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio.{Has, ULayer}
import io.getquill.{EntityQuery, Quoted}

import java.sql.SQLException
import javax.sql.DataSource

object QuillDemo extends zio.App {

  object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
    val dataSourceLayer: ULayer[Has[DataSource]] =
      DataSourceLayer.fromPrefix("quill").orDie
  }

  case class Person(id: Int, firstName: String, lastName: String, age: Int)
  
  import QuillContext._

  object Queries {
    val personOlderThan = quote { (age: Int) =>
      query[Person].filter(p => p.age > age)
    }
    val personNamed = quote { (name: String) =>
      query[Person].filter(p => p.firstName == name)
    }
    val allPersons = quote {
      query[Person]
    }
  }

  val composed: IO[SQLException, List[Person]] = {
    run(Queries.personOlderThan(lift(1))).provide(QuillContext.dataSourceLayer)
  }

  def program =
    for {
      _ <- zio.console.putStrLn("Hello, Quill!")
      res <- composed
      _ <- zio.console.putStrLn(res.toString)
    } yield ()

  override def run(args: List[String]) = program.exitCode

}