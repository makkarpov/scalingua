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

object Message {
  case class Singular(header: MessageHeader, context: Option[MultipartString], message: MultipartString,
                      translation: MultipartString) extends Message

  case class Plural(header: MessageHeader, context: Option[MultipartString], message: MultipartString,
                    plural: MultipartString, translations: Seq[MultipartString]) extends Message
}

sealed trait Message {
  def header: MessageHeader

  def context: Option[MultipartString]
  def message: MultipartString
}
