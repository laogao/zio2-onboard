package laogao

import zio.json.*

final case class CreateNote(title: String, content: String)

object CreateNote:
  given JsonCodec[CreateNote] = DeriveJsonCodec.gen[CreateNote]
