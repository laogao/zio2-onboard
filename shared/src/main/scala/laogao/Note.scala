package laogao

import zio.json._

final case class Note(id: String, title: String, content: String)

object Note:
  given JsonCodec[Note] = DeriveJsonCodec.gen[Note]
