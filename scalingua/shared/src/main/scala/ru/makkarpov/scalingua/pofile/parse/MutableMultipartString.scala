package ru.makkarpov.scalingua.pofile.parse

import ru.makkarpov.scalingua.pofile.MultipartString

import scala.collection.mutable

class MutableMultipartString {
  private var parts: mutable.Builder[String, Seq[String]] = _

  reset()

  def reset(): Unit = parts = Seq.newBuilder[String]
  def add(s: String): Unit = parts += s
  def result: MultipartString = MultipartString(parts.result():_*)
}
