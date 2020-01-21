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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.makkarpov.scalingua.OutputFormat

class StringUtilsTest extends AnyFlatSpec with Matchers {
  it should "escape strings" in {
    import ru.makkarpov.scalingua.StringUtils.{escape => f}

    // Samples where no escape is required:
    f("") shouldBe ""
    f("a string") shouldBe "a string"
    f("привет, мир!") shouldBe "привет, мир!"

    // Samples with simple one-letter escapes:
    f("\n") shouldBe "\\n"
    f("\\") shouldBe "\\\\"
    f("a string with \r, \n, \b, \f, \t, \', \", \\") shouldBe "a string with \\r, \\n, \\b, \\f, \\t, \', \\\", \\\\"

    // Samples with unicode escapes:
    f("⚡ high voltage ⚡") shouldBe "\\u26A1 high voltage \\u26A1"
    f("look! A cat: \uD83D\uDC31") shouldBe "look! A cat: \\uD83D\\uDC31"
  }

  it should "unescape strings" in {
    import ru.makkarpov.scalingua.StringUtils.{unescape => f}

    // Samples where string is treated literally:
    f("") shouldBe ""
    f("a string") shouldBe "a string"
    f("привет, мир!") shouldBe "привет, мир!"

    // Samples with simple one-letter escapes:
    f("\\n") shouldBe "\n"
    f("\\\\") shouldBe "\\"
    f("a string with \\r, \\n, \\b, \\f, \\t, \\', \\\", \\\\") shouldBe "a string with \r, \n, \b, \f, \t, \', \", \\"

    // Samples with unicode escapes:
    f("\\u26A1 high voltage \\u26A1") shouldBe "⚡ high voltage ⚡"
    f("look! A cat: \\uD83D\\uDC31") shouldBe "look! A cat: \uD83D\uDC31"
    f("\\u0000") shouldBe "\u0000"

    // Samples with invalid escapes:
    an [IllegalArgumentException] shouldBe thrownBy { f("\\x") }
    an [IllegalArgumentException] shouldBe thrownBy { f("\\") }
    an [IllegalArgumentException] shouldBe thrownBy { f("\\u") }
    an [IllegalArgumentException] shouldBe thrownBy { f("\\u000") }
    an [IllegalArgumentException] shouldBe thrownBy { f("\\uXXXX") }
  }

  it should "interpolate strings" in {
    import ru.makkarpov.scalingua.StringUtils.{interpolate => f}

    // Samples with literal strings:
    f[String]("") shouldBe ""
    f[String]("a string") shouldBe "a string"

    // Samples with escaped interpolation symbols:
    f[String]("%%") shouldBe "%"
    f[String]("%%%%") shouldBe "%%"
    f[String]("%%(notvar)") shouldBe "%(notvar)"
    f[String]("%%(test)%%") shouldBe "%(test)%"

    // Samples with interpolations:
    f[String]("%(a)", "a" -> "a") shouldBe "a"
    f[String]("%(a)%(a)%(a)", "a" -> "xy") shouldBe "xyxyxy"
    f[String]("%%%(a)%%%%(x)", "a" -> "zwx") shouldBe "%zwx%%(x)"
    f[String]("%(a)%(b)%(c)", "a" -> "1", "b" -> 2, "c" -> 3L) shouldBe "123"

    // Samples with invalid interpolations:
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%()", "" -> "") }
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%(") }
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%") }
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%(undefined)") }
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%[]") }
    an [IllegalArgumentException] shouldBe thrownBy { f[String]("%(a)%(b)", "a" -> true) }

    // Custom format interpolations:
    case class CStr(s: String)
    implicit val CStrFormat = new OutputFormat[CStr] {
      override def convert(s: String): CStr = CStr("{" + s + "}")
      override def escape(s: String): String = "[" + s + "]"
    }

    f[CStr]("a%(a)a%(a)a", "a" -> "a").s shouldBe "{a[a]a[a]a}"
  }

  it should "extract variables from strings" in {
    import ru.makkarpov.scalingua.StringUtils.{extractVariables => f}

    // Literal strings
    f("") shouldBe Set.empty
    f("a string") shouldBe Set.empty

    // Escaped placeholders, but still literal:
    f("%%") shouldBe Set.empty
    f("%%%%") shouldBe Set.empty
    f("%%(notvar)") shouldBe Set.empty
    f("%%(xx)%%") shouldBe Set.empty

    // With variables:
    f("%(a)") shouldBe Set("a")
    f("%(a)%(a)%(a)") shouldBe Set("a")
    f("%%%(a)%%%%(x)") shouldBe Set("a")
    f("%(a)%(b)%(c)") shouldBe Set("a", "b", "c")

    // Invalid:
    an [IllegalArgumentException] shouldBe thrownBy { f("%()") }
    an [IllegalArgumentException] shouldBe thrownBy { f("%(") }
    an [IllegalArgumentException] shouldBe thrownBy { f("%") }
    an [IllegalArgumentException] shouldBe thrownBy { f("%[]") }
  }
}
