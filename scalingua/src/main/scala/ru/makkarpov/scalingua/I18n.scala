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

import scala.language.experimental.macros

/**
  * Dynamic translation functions and the basic set of macro-based interpolators and translation functions.
  */
object I18n {
  implicit class StringInterpolator(val sc: StringContext) extends AnyVal {
    def t(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[String]) =
      macro Macros.interpolate[String]
  }

  def t(msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.singular[String]

  def tc(ctx: String, msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.singularCtx[String]

  def p(msg: String, msgPlural: String, n: Long, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.plural[String]

  def pc(ctx: String, msg: String, msgPlural: String, n: Long, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.pluralCtx[String]
}
