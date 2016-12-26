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

import ru.makkarpov.scalingua.plural.Suffix

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.internal.annotations.compileTimeOnly

trait I18n {
  type LString = LValue[String]

  val S = Suffix

  // Since I want to keep StringInterpolator AnyVal, I will extract it to a place where there is no path-dependency
  // and provide here only implicit conversion function.
  implicit def stringContext2Interpolator(sc: StringContext): I18n.StringInterpolator =
    new I18n.StringInterpolator(sc)

  implicit def long2MacroExtension(v: Long): I18n.PluralMacroExtensions =
    new I18n.PluralMacroExtensions(v)

  implicit def int2MacroExtension(i: Int): I18n.PluralMacroExtensions =
    new I18n.PluralMacroExtensions(i)

  implicit def string2SuffixExtension(s: String): Suffix.GenericSuffixExtension =
    new Suffix.GenericSuffixExtension(s)

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

object I18n extends I18n {
  class StringInterpolator(val sc: StringContext) extends AnyVal {
    def t(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
      macro Macros.interpolate[String]

    def lt(args: Any*)(implicit outputFormat: OutputFormat[String]): LString =
      macro Macros.lazyInterpolate[String]

    def p(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[String]): String =
      macro Macros.pluralInterpolate[String]

    def lp(args: Any*)(implicit outputFormat: OutputFormat[String]): LString =
      macro Macros.lazyPluralInterpolate[String]
  }

  class PluralMacroExtensions(val l: Long) extends AnyVal {
    def nVar: Long = throw new IllegalStateException(".nVars should not remain after macro expansion")
  }
}