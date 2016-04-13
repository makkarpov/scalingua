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

object LanguageId {
  private val languagePattern = "([a-zA-Z]{2,3})(?:[_-]([a-zA-Z]{2,3}))?".r

  /**
    * Creates `LanguageId` instance from language codes like `en` or `en-US`
    *
    * @param s Language code
    * @return `Some` with `LanguageId` instance if language code was parsed successfully, or `None` otherwise.
    */
  def get(s: String): Option[LanguageId] = s match {
    case languagePattern(lang, country) =>
      Some(LanguageId(lang.toLowerCase, if (country eq null) "" else country.toUpperCase))
    case _ => None
  }

  /**
    * Creates `LanguageId` instance from language codes like `en` or `en-US`
    *
    * @param s Language code
    * @return `LanguageId` instance
    */
  def apply(s: String): LanguageId = get(s).getOrElse(throw new IllegalArgumentException(s"Unrecognized language '$s'"))
}

/**
  * Class representing a pair of language and country (e.g. `en_US`)
  *
  * @param language ISO code of language
  * @param country ISO code of country, may be empty for generic languages.
  */
final case class LanguageId(language: String, country: String) {
  /**
    * @return Whether the country part is present
    */
  @inline def hasCountry = country.nonEmpty

  override def toString: String = language + (if (hasCountry) "-" else "") + country
}
