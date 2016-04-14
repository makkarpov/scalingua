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
import ru.makkarpov.scalingua.I18n._
import ru.makkarpov.scalingua.{Language, LanguageId}

class IStringTest extends FlatSpec with Matchers {
  class MockLang(s: String) extends Language {
    override def id: LanguageId = LanguageId("mock", "p" + s)
    override def singular(msgid: String): String = s"$s:$msgid"
    override def singular(msgctx: String, msgid: String): String = s"$s:$msgctx:$msgid"
    override def plural(msgid: String, msgidPlural: String, n: Long): String = s"$s:$msgid:$msgidPlural:$n"
    override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
      s"$s:$msgctx:$msgid:$msgidPlural:$n"
  }

  val mockLang1 = new MockLang("1")
  val mockLang2 = new MockLang("2")
  val mockLang3 = new MockLang("3")

  it should "handle internationalized strings when surrounding implicit lang is not present" in {
    val t = lt"Hello, world!"

    t.resolve(mockLang1) shouldBe "1:Hello, world!"
    t.resolve(mockLang2) shouldBe "2:Hello, world!"
  }

  it should "handle internationalized strings when implicit lang is present" in {
    implicit val lang = mockLang3

    val t = lt"12345"

    t.resolve(mockLang1) shouldBe "1:12345"
    t.resolve(mockLang2) shouldBe "2:12345"
    t.resolve shouldBe "3:12345"
  }
}
