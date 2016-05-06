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

package ru.makkarpov.scalingua.test

import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.{I18n, Language, Macros, OutputFormat}

import scala.language.experimental.macros

class CustomI18nTest extends FlatSpec with Matchers {
  case class CStr(s: String)

  implicit val CStrFormat = new OutputFormat[CStr] {
    override def convert(s: String): CStr = CStr(s"C{$s}")
    override def escape(s: String): String = s"[$s]"
  }

  object CustomI18n extends I18n {
    def ct(msg: String, args: (String, Any)*)(implicit lang: Language, outputFormat: OutputFormat[CStr]): CStr =
      macro Macros.singular[CStr]
  }

  implicit val mockLang = new MockLang("")

  import CustomI18n._

  it should "handle custom I18n classes via traits" in {
    t"Hello, world!" shouldBe "{s:Hello, world!}"
  }

  it should "handle custom methods in I18n classes" in {
    ct("Hello, world!").s shouldBe "C{{s:Hello, world!}}"
    ct("Hello, %(what)!", "what" -> "world").s shouldBe "C{{s:Hello, %(what)[[world]]!}}"

    """ ct("Hello, %(x)!", "y" -> 1) """ shouldNot compile
  }
}
