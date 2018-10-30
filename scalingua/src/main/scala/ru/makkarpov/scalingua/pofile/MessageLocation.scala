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

import java.io.File

object MessageLocation {
  implicit case object LocationOrdering extends Ordering[MessageLocation] {
    override def compare(x: MessageLocation, y: MessageLocation): Int = {
      //fs independent comparison
      val r = x.file.toString.compareTo(y.file.toString) match {
        case 0 => x.line.compare(y.line)
        case x => x
      }
      r
    }
  }

  def apply(file: String): MessageLocation = MessageLocation(new File(file), -1)
}

case class MessageLocation(file: File, line: Int) {
  def fileString: String = file.toString.replaceAllLiterally("""\""", "/")
}
