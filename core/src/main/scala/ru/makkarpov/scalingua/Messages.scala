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
  /**
    * Load all available languages that are compiled by SBT plugin.
    *
    * @param pkg Package to seek in. Must be equal to `localePackage` SBT setting.
    * @return A loaded `Messages`
    */
  def compiled(pkg: String = "locales"): Messages =
    try {
      val cls = Class.forName(pkg + (if (pkg.nonEmpty) "." else "") + "Languages$")
      cls.getField("MODULE$").get(null).asInstanceOf[Messages]
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Failed to load compiled languages from package '$pkg'", e)
    }
}

/**
  * Class representing a collection of language.
  *
  * @param langs Available languages
  */
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

  /**
    * Retrieves a language from message set by it's ID. The languages are tried in this order:
    *  1) Exact language, e.g. `ru_RU`
    *  2) Language matched only by language id, e.g. `ru_**`
    *  3) Fallback English language
    *
    * @param lang Language ID to fetch
    * @return Fetched language if available, or `Language.English` otherwise.
    */
  def apply(lang: LanguageId): Language = byCountry.getOrElse(lang, byLang.getOrElse(lang.language, Language.English))

  /**
    * Test whether this messages contains specified language, either exact (`ru_RU`) or fuzzy (`ru_**`).
    *
    * @param lang Language ID to test
    * @return Boolean indicating whether specified language is available
    */
  def contains(lang: LanguageId): Boolean = byCountry.contains(lang) || byLang.contains(lang.language)

  /**
    * Test whether this messages contains specified language exactly.
    *
    * @param lang
    * @return
    */
  def containsExact(lang: LanguageId): Boolean = byCountry.contains(lang)
}
