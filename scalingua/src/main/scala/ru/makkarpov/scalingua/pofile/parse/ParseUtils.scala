package ru.makkarpov.scalingua.pofile.parse

import scala.collection.mutable

object ParseUtils {
  // Interfacing with Scala from Java can be very unfriendly, so here are the utility methods for it.
  def none[T]: Option[T] = None
  def some[T](x: T): Option[T] = Option(x)
  def newBuilder[T]: mutable.Builder[T, Seq[T]] = Vector.newBuilder[T]
  def add[T](b: mutable.Builder[T, Seq[T]], x: T): Unit = b += x
}
