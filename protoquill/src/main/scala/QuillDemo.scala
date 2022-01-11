import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import zio.{ExitCode, Task}

object QuillDemo extends zio.App:

  val dbMigration = Task {
    val config = ConfigFactory.load
    val flyway = Flyway
      .configure()
      .dataSource(
        config.getString("quill.dataSource.url"),
        config.getString("quill.dataSource.user"),
        config.getString("quill.dataSource.password"))
      .locations("classpath:db/migration") // this is the default
      .load()
    flyway.migrate()
  }

  val quillExample = Task {
    import io.getquill.*
    case class Person(id: Int, firstName: String, lastName: String, age: Int)
    val ctx = new PostgresJdbcContext(SnakeCase, "quill")
    import ctx.*
    val named = "Joe"
    inline def somePeople = quote {
      query[Person].filter(p => p.firstName == lift(named))
    }
    val people: List[Person] = ctx.run(somePeople)
    println(people)
  }

  def program =
    for {
      _ <- dbMigration
      _ <- quillExample
    } yield ()

  override def run(args: List[String]) = program.exitCode

