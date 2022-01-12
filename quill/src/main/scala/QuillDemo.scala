import com.typesafe.config.ConfigFactory
import zio.{ExitCode, Task}

object QuillDemo extends zio.App {

  val quillExample = Task {
    import io.getquill._
    case class Person(id: Int, firstName: String, lastName: String, age: Int)
    val ctx = new PostgresJdbcContext(SnakeCase, "quill")
    import ctx._
    val named = "Joe"
    def somePeople = quote {
      query[Person].filter(p => p.firstName == lift(named))
    }

    val people: List[Person] = ctx.run(somePeople)
    println(people)
  }

  def program =
    for {
      _ <- quillExample
      _ <- zio.console.putStrLn("Hello, Quill!")
    } yield ()

  override def run(args: List[String]) = program.exitCode

}