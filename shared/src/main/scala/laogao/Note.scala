package laogao

import io.circe.generic.semiauto.*
import io.circe.Codec
import zio.json.*

final case class Note(id: String, title: String, content: String)

object Note:
  given Codec[Note] = deriveCodec[Note]
  given JsonCodec[Note] = DeriveJsonCodec.gen[Note]
