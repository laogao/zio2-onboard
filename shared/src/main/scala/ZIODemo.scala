import zio.console.{getStrLn, putStrLn}
import zio.{ExitCode, Task}
import zio.URIO
import zio.console.Console

object ZIODemo extends zio.App:

  case class Pilot(name: String)
  case class Ship(name: String, pilot: Pilot)
  given Pilot("Alex")
  given Roci(using pilot: Pilot): Ship = Ship("Roci", pilot)
  def flyTo(target: String)(using ship: Ship): Ship = {
    println(s"Flying to $target in $ship...")
    ship
  }
  val flight1 = flyTo("Mars")
  val flight2 = flyTo("Ganymede")
  println(s"Same ship? ${flight1 eq flight2}")
  println(s"Same pilot? ${flight1.pilot eq flight2.pilot}")

  val program =
    for {
      msg <- Task { flyTo("Mars") }
      _ <- putStrLn("Hello, " + msg + ", good to meet you!")
    } yield ()

  val dependencies = Console.live

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.exitCode


