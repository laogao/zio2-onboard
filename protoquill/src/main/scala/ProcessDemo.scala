import zio.console._
import zio.process._
import zio.stream.ZTransducer
import zio.stream.ZStream
import zio.Task

import java.util.UUID
import java.io.File
import zio.Schedule
import zio.ZIO
import zio.clock.Clock
import zio.Queue

object ProcessDemo extends zio.App {

  def initWD = Task {
    val path = s"/tmp/${UUID.randomUUID}"
    val file = new File(path)
    if (!file.exists) file.mkdir
    file
  }

  // using zio-process

  def runR(wd: File) = Command("R", "--vanilla", "--silent").workingDirectory(wd)

  val inputs = ZStream.fromIterable("\ngetwd()\nx <- 1\nx".getBytes)//.repeat(Schedule.spaced(10.seconds))

  import zio.duration._

  val program1 = for {
    wd <- initWD
    queue <- Queue.unbounded[Byte]
    _ <- queue.offerAll("\ngetwd()\nx <- 1\nx".getBytes)//.repeat(Schedule.spaced(10.seconds))
    // it seems that ProcessInput.fromStream requires you to `take` from the queue
    r1 <- runR(wd).stdin(ProcessInput.fromStream(ZStream.fromQueue(queue))).linesStream.tap(line => putStrLn(line)).runDrain.fork
    //r2 <- runR(wd).stdin(ProcessInput.fromStream(inputs)).linesStream.tap(line => putStrLn(line)).runDrain.fork
    _ <- (queue.offerAll("\nSys.time()\nx <- 1\nx".getBytes) *> queue.size.tap(s => putStrLn(s.toString))).repeat(Schedule.spaced(100.milliseconds))
    _ <- queue.shutdown
    _ <- r1.join
  } yield ()

  // using prox

  import io.github.vigoo.prox._
  import io.github.vigoo.prox.zstream._ 
  import io.github.vigoo.prox.path._

  implicit val runner: ProcessRunner[JVMProcessInfo] = new JVMProcessRunner 
  val prox = Process("R", List("--vanilla", "--silent"))

  val program2 = for {
    wd <- initWD
    _ <- putStrLn(wd.getAbsolutePath)
    queue <- Queue.unbounded[Byte]
    //res <- prox.!<(ZStream.fromQueue(queue)).in(root / "tmp" / wd.getName).>>(root / "tmp" / wd.getName / "out.log").run().fork
    res <- prox.!<(ZStream.fromQueue(queue)).in(root / "tmp" / wd.getName).run().fork
    _ <- queue.offerAll("\ngetwd()".getBytes)
    _ <- queue.offerAll("\nlibrary(ggplot2)".getBytes)
    _ <- queue.offerAll("\nggplot(data=mtcars,aes(x=wt,y=mpg))+geom_point()".getBytes)
    _ <- queue.offerAll("\nSys.time()".getBytes).repeat(Schedule.spaced(1000.milliseconds) && Schedule.recurs(10))
    _ <- queue.shutdown
    _ <- res.interrupt
    _ <- res.join
  } yield ()

  def run(args: List[String]) = program2.exitCode

  // // using vanilla scala.sys.process
  //
  // import scala.sys.process._
  // def typewriter(os: java.io.OutputStream): Unit = {
  //   os.write("x <- 1\nx\n".getBytes)
  //   os.flush()
  //   for (i <- 1 to 100) {
  //     Thread.sleep(1000)
  //     os.write(s"$i\n".getBytes)
  //     os.flush()
  //   }
  //   //os.close()
  // }
  // def trace(in: java.io.InputStream): Unit = {
  //   for (line <- scala.io.Source.fromInputStream(in).getLines()) {
  //     println(line)
  //   }
  // }
  // val pio = new ProcessIO(typewriter, trace, trace)
  // val proc = Seq("R", "--vanilla", "--silent").run(pio)
  // System.in.read()
}
