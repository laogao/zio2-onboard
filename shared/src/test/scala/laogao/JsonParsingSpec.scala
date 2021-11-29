package laogao

import zio.json.*

class JsonParsingSpec extends munit.FunSuite:

  test("parse Note") {
    val note = Note("123", "Scala.js is fun!", "Great times ahead.")
    val json = note.toJson.toString
    assertEquals(JsonDecoder[Note].decodeJson(json), Right(note))
  }

  test("parse Note sequence") {
    val note1 = Note("123", "Scala.js is fun!", "Great times ahead.")
    val note2 = Note("123", "Scala.js is fun!", "Great times ahead.")
    val notes: Seq[Note] = Seq(note1, note2)
    val json = notes.toJson.toString
    assertEquals(JsonDecoder[Seq[Note]].decodeJson(json), Right(notes))
  }

  test("parse CreateNote") {
    val createNote = CreateNote("Scala.js is fun!", "Great times ahead.")
    val json = createNote.toJson.toString
    assertEquals(JsonDecoder[CreateNote].decodeJson(json), Right(createNote))
  }
