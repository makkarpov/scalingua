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

package ru.makkarpov.scalingua.pofile

object MultipartString {
  val empty = MultipartString(Nil)
  def apply(s: String): MultipartString = MultipartString(s :: Nil)
}

case class MultipartString(parts: Seq[String]) {
  def merge = parts.mkString
  def isEmpty = parts.forall(_.isEmpty)

  override def toString = s"MultipartString(${parts.mkString(", ")})"
}
