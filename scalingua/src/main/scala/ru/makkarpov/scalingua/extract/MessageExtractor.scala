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

package ru.makkarpov.scalingua.extract

import ru.makkarpov.scalingua.Compat._

object MessageExtractor {
  private var session = Option.empty[ExtractorSession]

  def setupSession(c: Context): ExtractorSession = {
    val r =
      try {
        session match {
          case None => new ExtractorSession(c.universe, ExtractorSettings.fromContext(c))
          case Some(sess) if c.universe eq sess.global => sess
          case Some(sess) =>
            sess.finish()
            new ExtractorSession(c.universe, ExtractorSettings.fromContext(c))
        }
      } catch {
        case e: TaggedParseException => c.abort(c.enclosingPosition, e.getMessage)
      }

    session = Some(r)
    r
  }
}
