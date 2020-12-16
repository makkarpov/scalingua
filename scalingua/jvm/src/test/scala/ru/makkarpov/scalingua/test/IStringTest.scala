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
import ru.makkarpov.scalingua.I18n._

class IStringTest extends AnyFlatSpec with Matchers {
  val mockLang1 = new MockLang("1")
  val mockLang2 = new MockLang("2")
  val mockLang3 = new MockLang("3")

  it should "handle internationalized strings when surrounding implicit lang is not present" in {
    val t = lt"Hello, world!"

    t.resolve(mockLang1) shouldBe "{s1:Hello, world!}"
    t.resolve(mockLang2) shouldBe "{s2:Hello, world!}"
  }

  it should "handle internationalized strings when implicit lang is present" in {
    implicit val lang = mockLang3

    val t = lt"12345"

    t.resolve(mockLang1) shouldBe "{s1:12345}"
    t.resolve(mockLang2) shouldBe "{s2:12345}"
    t.resolve shouldBe "{s3:12345}"
  }
}
