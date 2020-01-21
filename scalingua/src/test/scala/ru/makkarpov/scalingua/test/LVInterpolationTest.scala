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
import ru.makkarpov.scalingua.LValue
import ru.makkarpov.scalingua.I18n._

class LVInterpolationTest extends AnyFlatSpec with Matchers {
  val langLvalue = new LValue[String](l => s"L'${l.id.toString}'")

  it should "interpolate LValues with correct languages" in {
    implicit val lang = new MockLang("1")

    val str = "Hello"

    t"$str, $langLvalue" shouldBe "{s1:%(str)[Hello], %(langLvalue)[L'mock-p1']}"
  }

  it should "generate LValues with nested LValues" in {
    val lstr = lt"Hello, $langLvalue"

    lstr(new MockLang("1")) shouldBe "{s1:Hello, %(langLvalue)[L'mock-p1']}"
    lstr(new MockLang("2")) shouldBe "{s2:Hello, %(langLvalue)[L'mock-p2']}"

    val l2 = lt"$langLvalue%(a) $langLvalue%(b)"

    l2(new MockLang("1")) shouldBe "{s1:%(a)[L'mock-p1'] %(b)[L'mock-p1']}"
  }
}
