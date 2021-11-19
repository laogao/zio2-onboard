package laogao

import scala.concurrent.{Future, ExecutionContext}
import scala.jdk.CollectionConverters.*

import java.nio.file.{Path, Paths, Files, StandardOpenOption}
import java.util.UUID

import zio.json.*

trait Repository extends NoteService

object Repository:

  def apply(directory: Path)(using ExecutionContext): Repository =
    if !Files.exists(directory) then Files.createDirectory(directory)
    new FileRepository(directory)

  private class FileRepository(directory: Path)(using ExecutionContext)
      extends Repository:
    def getAllNotes(): Future[Seq[Note]] = Future {
      val files = Files.list(directory).iterator.asScala
      files
        .filter(_.toString.endsWith(".json"))
        .map { file =>
          val bytes = Files.readAllBytes(file)
          JsonDecoder[Note].decodeJson(new String(bytes)).toOption.get
        }
        .toSeq
    }

    def createNote(title: String, content: String): Future[Note] = Future {
      val id = UUID.randomUUID().toString
      val note = Note(id, title, content)
      val file = directory.resolve(s"$id.json")
      val bytes = note.toJson.getBytes
      Files.write(file, bytes, StandardOpenOption.CREATE)
      note
    }
