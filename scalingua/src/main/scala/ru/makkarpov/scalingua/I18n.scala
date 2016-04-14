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

object I18n {
  type LString = LValue[String]

  implicit class StringInterpolator(val sc: StringContext) extends AnyVal {
    def t(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
      macro Macros.interpolate[String]

    def lt(args: Any*)(implicit outputFormat: OutputFormat[String]): LString =
      macro Macros.lazyInterpolate[String]
  }

  def t(msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.singular[String]

  def lt(msg: String, args: (String, Any)*)(implicit outputFormat: OutputFormat[String]): LString =
    macro Macros.lazySingular[String]

  def tc(ctx: String, msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.singularCtx[String]

  def ltc(ctx: String, msg: String, args: (String, Any)*)(implicit outputFormat: OutputFormat[String]): LString =
    macro Macros.lazySingularCtx[String]

  def p(msg: String, msgPlural: String, n: Long, args: (String, Any)*)
       (implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.plural[String]

  def lp(msg: String, msgPlural: String, n: Long, args: (String, Any)*)
        (implicit outputFormat: OutputFormat[String]): LString =
    macro Macros.lazyPlural[String]

  def pc(ctx: String, msg: String, msgPlural: String, n: Long, args: (String, Any)*)
       (implicit lang: Language, outputFormat: OutputFormat[String]): String =
    macro Macros.pluralCtx[String]

  def lpc(ctx: String, msg: String, msgPlural: String, n: Long, args: (String, Any)*)
        (implicit outputFormat: OutputFormat[String]): LString =
    macro Macros.lazyPluralCtx[String]
}
