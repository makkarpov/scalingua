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

    t"Number is $x" shouldBe s"{s:Number is %(x)[10]}"
    t"Number is $x%(y)" shouldBe s"{s:Number is %(y)[10]}"

    """ t"Number is ${x+10}" """ shouldNot typeCheck
    t"Number is ${x+10}%(y)" shouldBe s"{s:Number is %(y)[20]}"
   }

  it should "handle singular forms" in {
    t("Hello, world!") shouldBe "{s:Hello, world!}"

    """ t("Test %(x)") """ shouldNot typeCheck
    """ t("Test", "x" -> 20) """ shouldNot typeCheck
    """ t("Test %(y)", "x" -> 20) """ shouldNot typeCheck

    t("Test %(x)", "x" -> 20) shouldBe "{s:Test %(x)[20]}"
    t("Test %(x)", ("x", 30)) shouldBe "{s:Test %(x)[30]}"
    t("Test %(x)%(x)%(x)", "x" -> true) shouldBe "{s:Test %(x)[true]%(x)[true]%(x)[true]}"

    """ val s = "1"; t(s) """ shouldNot typeCheck
    """ val s = "x"; t("1", s -> 1) """ shouldNot typeCheck

    { val i = 2; t("1%(1)3", "1" -> i ) } shouldBe "{s:1%(1)[2]3}"
  }

  it should "handle contextual singular forms" in {
    tc("test", "Hello, world!") shouldBe "{sc:test:Hello, world!}"

    """ tc("1", "%(x)") """ shouldNot typeCheck
    """ tc("1", "2", "3" -> 4) """ shouldNot typeCheck
    """ tc("1", "%(2)", "3" -> 4) """ shouldNot typeCheck

    tc("1", "%(2)", "2" -> 3) shouldBe "{sc:1:%(2)[3]}"
    tc("1", "%(2)%(3)", "2" -> 4, "3" -> 5) shouldBe "{sc:1:%(2)[4]%(3)[5]}"

    """ val s = "1"; tc(s, "1") """ shouldNot typeCheck
    """ val s = "2"; tc("1", s) """ shouldNot typeCheck
    """ val s = "3"; tc("1", "2", s -> 4) """ shouldNot typeCheck

    { val i = 3; tc("1", "%(2)", "2" -> i) } shouldBe "{sc:1:%(2)[3]}"
  }

  it should "handle plural forms" in {
    p("Hello, %(n) world!", "Hello, %(n) worlds!", 1) shouldBe "{p:Hello, %(n)[1] world!:Hello, %(n)[1] worlds!:1}"

    // %(n) should be present in string
    """ p("Test", "Tests", 1) """ shouldNot typeCheck

    val n = 10 + 20
    p("%(n)", "%(n)s", n) shouldBe "{p:%(n)[30]:%(n)[30]s:30}"

    """ p("1%(n)", "2", 3) """ shouldNot typeCheck
    """ p("1", "2%(n)", 3) """ shouldNot typeCheck
    """ p("1%(n)%(x)", "2%(n)", 3) """ shouldNot typeCheck
    """ p("1%(n)", "2%(n)%(x)", 3) """ shouldNot typeCheck

    p("%(n)s%(x)", "%(x)p%(n)", 3, "x" -> 2) shouldBe "{p:%(n)[3]s%(x)[2]:%(x)[2]p%(n)[3]:3}"

    """ p("1%(n)%(x)", "2%(n)%(y)", 3, "x" -> 1, "y" -> 2) """ shouldNot typeCheck
    """ val s = "%(n)"; p(s, "%(n)", 3) """ shouldNot typeCheck
    """ val s = "%(n)"; p("%(n)", s, 3) """ shouldNot typeCheck
  }

  it should "handle contextual plural forms" in {
    pc("1", "2%(n)", "%(n)3", 4) shouldBe "{pc:1:2%(n)[4]:%(n)[4]3:4}"

    """ pc("0", "1", "2%(n)", 3) """ shouldNot typeCheck

    """ pc("0", "1%(n)", "2", 3) """ shouldNot typeCheck
    """ pc("0", "1", "2%(n)", 3) """ shouldNot typeCheck
    """ pc("0", "1%(n)%(x)", "2%(n)", 3) """ shouldNot typeCheck
    """ pc("0", "1%(n)", "2%(n)%(x)", 3) """ shouldNot typeCheck

    pc("1", "%(x)%(y)%(n)%(x)", "%(y)%(n)%(x)%(y)", 1, "x" -> 2, "y" -> 3) shouldBe
          "{pc:1:%(x)[2]%(y)[3]%(n)[1]%(x)[2]:%(y)[3]%(n)[1]%(x)[2]%(y)[3]:1}"

    """ val s = "1"; pc(s, "%(n)", "%(n)", 1) """ shouldNot typeCheck
    """ val s = "%(n)"; pc("1", s, "%(n)", 1) """ shouldNot typeCheck
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

    """ val c = '1'; t("1".stripMargin(c)) """ shouldNot typeCheck
    """ val s = "1"; t(s.stripMargin('1')) """ shouldNot typeCheck
  }

  it should "handle plural strings" in {
    val n = 10
    val k = 5L
    val s = "black"

    p"I have $n fox${S.es}" shouldBe "{p:I have %(n)[10] fox:I have %(n)[10] foxes:10}"
    p"I have $k $s cat${S.s}" shouldBe "{p:I have %(k)[5] %(s)[black] cat:I have %(k)[5] %(s)[black] cats:5}"
    p"Msg $n" shouldBe "{p:Msg %(n)[10]:Msg %(n)[10]:10}"

    // Multiple integer variables, each could be a plural number:
    """ p"test $n $n" """ shouldNot typeCheck
    """ p"$k test $n" """ shouldNot typeCheck

    // Multiple `.nVar` candidates:
    """ p"${n.nVar} ${k.nVar}" """ shouldNot typeCheck
    """ p"${n.nVar} ${n.nVar}" """ shouldNot typeCheck

    // No `nVar`s:
    """ p"Simple string" """ shouldNot typeCheck
    """ p"$s str" """ shouldNot typeCheck
    """ p"${S.s}${S.es}" """ shouldNot typeCheck

    // Many many many suffixes:
    p"${S.s}${S.s}${S.es}${S.es}${S.s}${S.es}$n" shouldBe "{p:%(n)[10]:ssesesses%(n)[10]:10}"

    // Adjust suffix to case:
    p"I HAVE $n CAT${S.s}" shouldBe "{p:I HAVE %(n)[10] CAT:I HAVE %(n)[10] CATS:10}"
    p"$n CAT${S.s} cat${S.s} fox${S.es} FOX${S.es}" shouldBe
        "{p:%(n)[10] CAT cat fox FOX:%(n)[10] CATS cats foxes FOXES:10}"

    // `a &> b` operator:
    p"There ${"is" &> "are"} $n pen${S.s}" shouldBe "{p:There is %(n)[10] pen:There are %(n)[10] pens:10}"
    """ p"${p &> "1"}" """ shouldNot typeCheck
    """ p"${"1" &> p}" """ shouldNot typeCheck

    p"${S.s}${"" &> ""}${"x" &> ""}${"" &> "y"}${S.es}${"z" &> "w"}$n" shouldBe "{p:xz%(n)[10]:syesw%(n)[10]:10}"
  }
}
