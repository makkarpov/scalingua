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
      cmt.comment.trim.split(":") match {
        case Array(file, line) =>
          try locations += MessageLocation(file, line.toInt)
          catch {
            case _: NumberFormatException => throw ParserException(left, right, "cannot parse line number")
          }

        case Array(file) => locations += MessageLocation(file, -1)

        case _ => throw ParserException(left, right, "incorrect location format")
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
