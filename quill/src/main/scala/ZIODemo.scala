import zio.Console.{printLine, readLine}
import zio.ZIOAppDefault

object ZIODemo extends ZIOAppDefault:
  def run =
    for {
      _ <- printLine("Hello! What is your name?")
      n <- readLine
      _ <- printLine("Hello, " + n + ", good to meet you!")
    } yield ()
