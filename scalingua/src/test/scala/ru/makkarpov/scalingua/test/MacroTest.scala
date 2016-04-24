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
import ru.makkarpov.scalingua.plural.Suffix

class MacroTest extends FlatSpec with Matchers {
  implicit val mockLang = new MockLang("")

  it should "handle string interpolations" in {
    t"Hello, world!" shouldBe "{s:Hello, world!}"

    val x = 10

    t"Number is $x" shouldBe s"{s:Number is $x}"
    t"Number is $x%(y)" shouldBe s"{s:Number is $x}"

    """ t"Number is ${x+10}" """ shouldNot compile
    t"Number is ${x+10}%(y)" shouldBe s"{s:Number is ${x+10}}"
   }

  it should "handle singular forms" in {
    t("Hello, world!") shouldBe "{s:Hello, world!}"

    """ t("Test %(x)") """ shouldNot compile
    """ t("Test", "x" -> 20) """ shouldNot compile
    """ t("Test %(y)", "x" -> 20) """ shouldNot compile

    t("Test %(x)", "x" -> 20) shouldBe "{s:Test 20}"
    t("Test %(x)", ("x", 30)) shouldBe "{s:Test 30}"
    t("Test %(x)%(x)%(x)", "x" -> true) shouldBe "{s:Test truetruetrue}"

    """ val s = "1"; t(s) """ shouldNot compile
    """ val s = "x"; t("1", s -> 1) """ shouldNot compile

    { val i = 2; t("1%(1)3", "1" -> i ) } shouldBe "{s:123}"
  }

  it should "handle contextual singular forms" in {
    tc("test", "Hello, world!") shouldBe "{sc:test:Hello, world!}"

    """ tc("1", "%(x)") """ shouldNot compile
    """ tc("1", "2", "3" -> 4) """ shouldNot compile
    """ tc("1", "%(2)", "3" -> 4) """ shouldNot compile

    tc("1", "%(2)", "2" -> 3) shouldBe "{sc:1:3}"
    tc("1", "%(2)%(3)", "2" -> 4, "3" -> 5) shouldBe "{sc:1:45}"

    """ val s = "1"; tc(s, "1") """ shouldNot compile
    """ val s = "2"; tc("1", s) """ shouldNot compile
    """ val s = "3"; tc("1", "2", s -> 4) """ shouldNot compile

    { val i = 3; tc("1", "%(2)", "2" -> i) } shouldBe "{sc:1:3}"
  }

  it should "handle plural forms" in {
    p("Hello, %(n) world!", "Hello, %(n) worlds!", 1) shouldBe "{p:Hello, 1 world!:Hello, 1 worlds!:1}"

    // %(n) should be present in string
    """ p("Test", "Tests", 1) """ shouldNot compile

    val n = 10 + 20
    p("%(n)", "%(n)s", n) shouldBe "{p:30:30s:30}"

    """ p("1%(n)", "2", 3) """ shouldNot compile
    """ p("1", "2%(n)", 3) """ shouldNot compile
    """ p("1%(n)%(x)", "2%(n)", 3) """ shouldNot compile
    """ p("1%(n)", "2%(n)%(x)", 3) """ shouldNot compile

    p("%(n)s%(x)", "%(x)p%(n)", 3, "x" -> 2) shouldBe "{p:3s2:2p3:3}"

    """ p("1%(n)%(x)", "2%(n)%(y)", 3, "x" -> 1, "y" -> 2) """ shouldNot compile
    """ val s = "%(n)"; p(s, "%(n)", 3) """ shouldNot compile
    """ val s = "%(n)"; p("%(n)", s, 3) """ shouldNot compile
  }

  it should "handle contextual plural forms" in {
    pc("1", "2%(n)", "%(n)3", 4) shouldBe "{pc:1:24:43:4}"

    """ pc("0", "1", "2%(n)", 3) """ shouldNot compile

    """ pc("0", "1%(n)", "2", 3) """ shouldNot compile
    """ pc("0", "1", "2%(n)", 3) """ shouldNot compile
    """ pc("0", "1%(n)%(x)", "2%(n)", 3) """ shouldNot compile
    """ pc("0", "1%(n)", "2%(n)%(x)", 3) """ shouldNot compile

    pc("1", "%(x)%(y)%(n)%(x)", "%(y)%(n)%(x)%(y)", 1, "x" -> 2, "y" -> 3) shouldBe "{pc:1:2312:3123:1}"

    """ val s = "1"; pc(s, "%(n)", "%(n)", 1) """ shouldNot compile
    """ val s = "%(n)"; pc("1", s, "%(n)", 1) """ shouldNot compile
  }

  it should "handle multiline strings" in {
    t"""1
2
3""" shouldBe "{s:1\n2\n3}"

    t"""1
       |2""" shouldBe "{s:1\n       |2}"

    t("""1
2
3""") shouldBe "{s:1\n2\n3}"


    t("""1
        |2
        |3""".stripMargin) shouldBe "{s:1\n2\n3}"


    t("""1
        #2
        #3
        """.stripMargin('#')) shouldBe "{s:1\n2\n3}"

    t("""1
        |2
      """.stripMargin('#')) shouldBe "{s:1\n        |2}"

    t("1".stripMargin('1')) shouldBe "{s:}"

    """ val c = '1'; t("1".stripMargin(c)) """ shouldNot compile
    """ val s = "1"; t(s.stripMargin('1')) """ shouldNot compile
  }

  it should "handle plural strings" in {
    val n = 10
    val k = 5L
    val s = "black"

    p"I have $n fox${S.es}" shouldBe "{p:I have 10 fox:I have 10 foxes:10}"
    p"I have $k $s cat${S.s}" shouldBe "{p:I have 5 black cat:I have 5 black cats:5}"
    p"Msg $n" shouldBe "{p:Msg 10:Msg 10:10}"

    // Multiple integer variables, each could be a candidate for plural number:
    """ p"test $n $n" """ shouldNot compile
    """ p"$k test $n" """ shouldNot compile

    // Multiple `.nVar` candidates:
    """ p"${n.nVar} ${k.nVar}" """ shouldNot compile
    """ p"${n.nVar} ${n.nVar}" """ shouldNot compile

    // No `nVar`s:
    """ p"Simple string" """ shouldNot compile
    """ p"$s str" """ shouldNot compile
    """ p"${S.s}${S.es}" """ shouldNot compile

    // Many many many suffixes:
    p"${S.s}${S.s}${S.es}${S.es}${S.s}${S.es}$n" shouldBe "{p:10:ssesesses10:10}"

    // Adjust suffix to case:
    p"I HAVE $n CAT${S.s}" shouldBe "{p:I HAVE 10 CAT:I HAVE 10 CATS:10}"
    p"$n CAT${S.s} cat${S.s} fox${S.es} FOX${S.es}" shouldBe "{p:10 CAT cat fox FOX:10 CATS cats foxes FOXES:10}"
  }
}
