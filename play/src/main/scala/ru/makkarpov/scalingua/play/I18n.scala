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

package ru.makkarpov.scalingua.play

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import ru.makkarpov.scalingua
import ru.makkarpov.scalingua._

import scala.language.experimental.macros
import scala.language.implicitConversions

trait I18n extends scalingua.I18n {
  type LHtml = LValue[Html]

  implicit val htmlOutputFormat = new OutputFormat[Html] {
    override def convert(s: String): Html = Html(s)

    override def escape(s: String): String = {
      val ret = new StringBuilder
      var i = 0
      ret.sizeHint(s.length)
      while (i < s.length) {
        s.charAt(i) match {
          case '<' => ret.append("&lt;")
          case '>' => ret.append("&gt;")
          case '"' => ret.append("&quot;")
          case '\'' => ret.append("&#x27;")
          case '&' => ret.append("&amp;")
          case c => ret += c
        }
        i += 1
      }
      ret.result()
    }
  }

  // The conversion from `RequestHeader` to `LanguageId` will imply information loss:
  // Suppose browser sent the following language precedence list `xx_YY`, `zz_WW`, `en_US`.
  // This conversion will have no information about available languages, so it will result in
  // `LanguageId("xx", "YY")` even if this language is absent. By supplying implicit `Messages`
  // too, we will have enough information to skip unsupported languages and return supported
  // one with respect to priority.

  implicit def requestHeader2Language(implicit rq: RequestHeader, msg: Messages): Language = {
    val langs = (for {
      value0 <- rq.headers(HeaderNames.ACCEPT_LANGUAGE).split(',')
      value = value0.trim
    } yield {
      RequestHeader.qPattern.findFirstMatchIn(value) match {
        case Some(m) => (m.group(1).toDouble, m.before.toString)
        case None => (1.0, value) // “The default value is q=1.”
      }
    }).sortBy(_._1).iterator

    while (langs.hasNext) {
      val (_, id) = langs.next()
      val lng = LanguageId.get(id)

      if (lng.isDefined && msg.contains(lng.get))
        return msg(lng.get)
    }

    Language.English
  }

  implicit def stringContext2Interpolator1(sc: StringContext): I18n.StringInterpolator =
    new I18n.StringInterpolator(sc)

  def th(msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
    macro Macros.singular[Html]

  def lth(msg: String, args: (String, Any)*)(implicit outputFormat: OutputFormat[Html]): LHtml =
    macro Macros.lazySingular[Html]

  def tch(ctx: String, msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
    macro Macros.singularCtx[Html]

  def ltch(ctx: String, msg: String, args: (String, Any)*)(implicit outputFormat: OutputFormat[Html]): LHtml =
    macro Macros.lazySingularCtx[Html]

  def p(msg: String, msgPlural: String, n: Long, args: (String, Any)*)
       (implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
    macro Macros.plural[Html]

  def lp(msg: String, msgPlural: String, n: Long, args: (String, Any)*)
        (implicit outputFormat: OutputFormat[Html]): LHtml =
    macro Macros.lazyPlural[Html]

  def pc(ctx: String, msg: String, msgPlural: String, n: Long, args: (String, Any)*)
        (implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
    macro Macros.pluralCtx[Html]

  def lpc(ctx: String, msg: String, msgPlural: String, n: Long, args: (String, Any)*)
         (implicit outputFormat: OutputFormat[Html]): LHtml =
    macro Macros.lazyPluralCtx[Html]
}

object I18n extends I18n {
  class StringInterpolator(val sc: StringContext) extends AnyVal {
    def h(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
      macro Macros.interpolate[Html]

    def lh(args: Any*)(implicit outputFormat: OutputFormat[Html]): LHtml =
      macro Macros.lazyInterpolate[Html]
  }
}