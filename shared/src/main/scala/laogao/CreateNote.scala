package laogao

import io.circe.generic.semiauto.*
import io.circe.Codec
import zio.json.*

final case class CreateNote(title: String, content: String)

object CreateNote:
  given Codec[CreateNote] = deriveCodec[CreateNote]
  given JsonCodec[CreateNote] = DeriveJsonCodec.gen[CreateNote]
