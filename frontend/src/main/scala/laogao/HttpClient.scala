package laogao

import org.scalajs.dom.*
import scala.scalajs.js

import java.io.IOException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import zio.json.*

class HttpClient(using ExecutionContext) extends NoteService:

  def getAllNotes(): Future[Seq[Note]] =
    for
      resp <- Fetch.fetch("./api/notes").toFuture
      json <- resp.jsonOrFailure
    yield JsonDecoder[Seq[Note]].decodeJson(json.toString).toOption.get

  def createNote(title: String, content: String): Future[Note] =
    val request = Request(
      "./api/notes",
      new:
        method = HttpMethod.POST
        headers = js.Dictionary("Content-Type" -> "application/json")
        body = CreateNote(title, content).toJson
    )
    for
      resp <- Fetch.fetch(request).toFuture
      json <- resp.jsonOrFailure
    yield JsonDecoder[Note].decodeJson(json.toString).toOption.get

  extension (resp: Response)
    private def jsonOrFailure: Future[js.Any] =
      if resp.ok then resp.json().toFuture
      else Future.failed(new IOException(resp.statusText))
