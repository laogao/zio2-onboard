import zio.Console.{printLine, readLine}
import zio.ZIOAppDefault

object QuillDemo extends ZIOAppDefault:

  def run =
    import io.getquill.*
    val ctx = SqlMirrorContext(MirrorSqlDialect, Literal)
    import ctx.*
    val pi = quote(3.1415926)
    case class Circle(radius: Float)
    val area = quote { (c: Circle) => { val r2 = c.radius * c.radius ; pi * r2 } }
    val areas = quote { query[Circle].map(c => pi * c.radius * c.radius) }
    println(area)
    println(areas)
    for {
      _ <- printLine("Hello, world!")
    } yield ()
