package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.ComplexSymbolFactory.Location

import ru.makkarpov.scalingua.pofile.{MessageFlag, MessageHeader, MessageLocation}

import scala.collection.mutable

class MutableHeader {
  private var _startLoc: Location = _
  private var _endLoc: Location = _

  private var comments: mutable.Builder[String, Seq[String]] = _
  private var extractedComments: mutable.Builder[String, Seq[String]] = _
  private var locations: mutable.Builder[MessageLocation, Seq[MessageLocation]] = _
  private var flags: MessageFlag.ValueSet = _
  private var tag: Option[String] = _

  private def parseComment(cmt: Comment, left: Location, right: Location): Unit = cmt.commentTag match {
    case ' ' => comments += cmt.comment.trim
    case '.' => extractedComments += cmt.comment.trim
    case ':' =>
      // It seems that GNU .po utilities can combine locations in a single line:
      //   #: some.file:123 other.file:456
      // but specifications does not specify how to handle spaces in a string.
      // So ignore there references, Scalingua itself will never produce such lines.
      val str = cmt.comment.trim
      val idx = str.lastIndexOf(':')
      if (idx != -1) {
        val file = str.substring(0, idx)
        val line =
          try str.substring(idx + 1)
          catch {
            case _: NumberFormatException => throw ParserException(left, right, "cannot parse line number")
          }

        locations += MessageLocation(file, line.toInt)
      } else {
        locations += MessageLocation(str, -1)
      }

    case ',' =>
      val addFlags = cmt.comment.trim.split(",").flatMap { s =>
        try Some(MessageFlag.withName(s.toLowerCase))
        catch { case _: NoSuchElementException => None }
      }

      flags = addFlags.foldLeft(flags)(_ + _)

    case '~' => tag = Some(cmt.comment.trim)

    case _ => // ignore
  }

  def reset(): Unit = {
    _startLoc = null
    _endLoc = null

    comments = Vector.newBuilder
    extractedComments = Vector.newBuilder
    locations = Vector.newBuilder
    flags = MessageFlag.ValueSet()
    tag = None
  }

  def add(cmt: Comment, left: Location, right: Location): Unit = {
    if (_startLoc == null) {
      _startLoc = left
    }

    _endLoc = right
    parseComment(cmt, left, right)
  }

  def result(): MessageHeader =
    MessageHeader(comments.result(), extractedComments.result(), locations.result(), flags, tag)
}
