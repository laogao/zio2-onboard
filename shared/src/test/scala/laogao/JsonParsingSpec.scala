package laogao

import io.circe.parser
import io.circe.syntax.*
import io.circe.Printer

class JsonParsingSpec extends munit.FunSuite:
  private val printer: Printer = Printer(
    dropNullValues = true,
    indent = ""
  )

  test("parse Note") {
    val note = Note("123", "Scala.js is fun!", "Great times ahead.")
    val json = printer.print(note.asJson)
    assertEquals(parser.decode[Note](json), Right(note))
  }
