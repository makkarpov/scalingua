/******************************************************************************
 * Copyright © 2016 Maxim Karpov                                              *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package ru.makkarpov.scalingua

object InsertableIterator {
  implicit class IteratorExtensions[T](val it: Iterator[T]) extends AnyVal {
    def insertable = new InsertableIterator[T](it)
  }
}

class InsertableIterator[T](backing: Iterator[T]) extends Iterator[T] with BufferedIterator[T] {
  private var queue = List.empty[T]

  override def hasNext: Boolean = queue.nonEmpty || backing.hasNext

  override def next(): T = queue match {
    case Nil => backing.next()
    case head :: rest =>
      queue = rest
      head
  }

  override def head: T = queue match {
    case Nil =>
      val el = backing.next()
      queue ::= el
      el

    case head :: _ => head
  }

  def unnext(t: T): Unit = queue ::= t
}
