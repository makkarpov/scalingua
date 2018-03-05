package ru.makkarpov.scalingua.extract

import java.io.{File, FileInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import com.grack.nanojson.{JsonObject, JsonParser, JsonParserException}
import ru.makkarpov.scalingua.pofile.Message.{Plural, Singular}
import ru.makkarpov.scalingua.pofile._
import scala.collection.JavaConverters._

object TaggedParser {
  val TaggedFileName = "tagged-messages.json"

  case class TaggedMessage(tag: String, msg: String, plural: Option[String], comment: Seq[String]) {
    def toMessage: Message = {
      val header = MessageHeader(comment, Nil, MessageLocation(TaggedFileName) :: Nil, MessageFlag.empty, Some(tag))

      plural match {
        case None => Singular(header, None, MultipartString(msg), MultipartString.empty)
        case Some(p) => Plural(header, None, MultipartString(msg), MultipartString(p),
          Seq(MultipartString.empty, MultipartString.empty))
      }
    }
  }

  /*
   * Format for tagged JSON file:
   *
   * {
   *  "some.message.tag": {
   *    "message": "...", // message itself, mandatory
   *    "plural": "...", // plural version of message, optional
   *    "comments": [ "...", "..."] // comments, optional
   *  },
   *
   *  // or, simply:
   *  "some.other.message.tag": "message"
   * }
   */
  def parse(f: File): Seq[TaggedMessage] = {
    val ret = Vector.newBuilder[TaggedMessage]

    try {
      val obj = {
        val r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)
        try JsonParser.`object`().from(r) finally r.close()
      }

      for (k <- obj.keySet().asScala) obj.get(k) match {
        case v: JsonObject =>
          if (!v.has("message"))
            throw TaggedParseException(s"Object with key '$k' has no 'message' field")

          if (!v.isString("message"))
            throw TaggedParseException(s"Object with key '$k' has non-string 'message' field")

          val msg = v.getString("message")

          val plural =
            if (v.has("plural")) {
              if (!v.isString("plural"))
                throw TaggedParseException(s"Object with key '$k' has non-string 'plural' field")
              Some(v.getString("plural"))
            } else None

          val comments =
            if (v.has("comments")) {
              if (v.isString("comments")) v.getString("comments") :: Nil
              else v.getArray("comments").asScala.map(_.asInstanceOf[String])
            } else Nil

          ret += TaggedMessage(k, msg, plural, comments)

        case v: String =>
          ret += TaggedMessage(k, v, None, Nil)
      }
    } catch {
      case e: JsonParserException =>
        throw new TaggedParseException(s"Tagged JSON syntax error at ${f.getCanonicalPath}:${e.getLinePosition}:${e.getCharPosition}", e)
    }

    ret.result()
  }
}
