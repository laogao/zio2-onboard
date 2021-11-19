package laogao

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}

import zio.json.*

trait JsonSupport:
  given [T: JsonDecoder]: FromEntityUnmarshaller[T] =
    Unmarshaller.stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`)
      .map(JsonDecoder[T].decodeJson(_).toOption.get)

  given [T: JsonEncoder]: ToEntityMarshaller[T] =
    Marshaller
      .stringMarshaller(MediaTypes.`application/json`)
      .compose(value => JsonEncoder[T].encodeJson(value, None).toString)


