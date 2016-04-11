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

object Messages {
  def compiled(pkg: String = "locales"): Messages =
    try {
      val cls = Class.forName(pkg + (if (pkg.nonEmpty) "." else "") + "Languages$")
      cls.getField("MODULE$").get(null).asInstanceOf[Messages]
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Failed to load compiled languages from package '$pkg'", e)
    }
}

class Messages(langs: Language*) {
  private val (byLang, byCountry) = {
    val lng = Map.newBuilder[String, Language]
    val cntr = Map.newBuilder[LanguageId, Language]
    var lngs = Set.empty[String]

    for (l <- langs) {
      if (!lngs.contains(l.id.language)) {
        lng += l.id.language -> l
        lngs += l.id.language
      }

      cntr += l.id -> l
    }

    (lng.result(), cntr.result())
  }

  def apply(lang: LanguageId): Language = byCountry.getOrElse(lang, byLang.getOrElse(lang.language, Language.English))
}
