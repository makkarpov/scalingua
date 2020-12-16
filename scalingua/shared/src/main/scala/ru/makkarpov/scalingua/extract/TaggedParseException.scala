package ru.makkarpov.scalingua.extract

case class TaggedParseException(msg: String) extends RuntimeException(msg) {
  def this(msg: String, cause: Throwable) = {
    this(msg)
    initCause(cause)
  }
}
