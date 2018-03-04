package ru.makkarpov.scalingua.plugin

case class ParseFailedException(message: String, cause: Throwable) extends Exception(message, cause)
with sbt.UnprintableException {

}
