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
import ru.makkarpov.scalingua.plural.Expression

class ParserTest extends FlatSpec with Matchers {
  import ru.makkarpov.scalingua.plural.Parser.{apply => p}

  it should "evaluate simple expressions" in {
    p("0").eval(0) shouldBe 0
    p("1").eval(0) shouldBe 1
    p("0").eval(1) shouldBe 0
    p("3").eval(999) shouldBe 3
    p("n;").eval(0) shouldBe 0
    p("n;").eval(10) shouldBe 10
  }

  it should "evaluate simple unary expresions" in {
    p("-0").eval(0) shouldBe 0
    p("~0").eval(0) shouldBe (~0L)
    p("+0").eval(0) shouldBe 0
    p("!2").eval(0) shouldBe 0
    p("!0").eval(0) shouldBe 1
    p("-n").eval(10) shouldBe -10
  }

  it should "evaluate expressions with operators of single precedence" in {
    p("2+2+2+2+2").eval(0) shouldBe 10
    p("2-2+2-2+2").eval(0) shouldBe 2
    p("2*2*2*2*2").eval(0) shouldBe 32
    p("2/2*2/2*2").eval(0) shouldBe 2
    p("1&1&1&1&1").eval(0) shouldBe 1
    p("1&0&1&1&1").eval(0) shouldBe 0
  }

  it should "evaluate expressions with operators of mixed precedence" in {
    p("2*2+2/2-2").eval(0) shouldBe 3
    p("2*2+1&2-2").eval(0) shouldBe 0
    p("1*3<<1==n").eval(0) shouldBe 0
    p("1*3<<1==n").eval(6) shouldBe 1
    p("-1 + -3").eval(0) shouldBe -4
    p("1+ +3").eval(0) shouldBe 4
  }

  it should "evaluate expressions with parentheses" in {
    p("(1)").eval(0) shouldBe 1
    p("(((1)))").eval(0) shouldBe 1
    p("(2+2)*(2+2)+2").eval(0) shouldBe 18
    p("((2 == 2)+1)*3").eval(0) shouldBe 6
  }

  it should "evaluate ternary operator" in {
    p("n ? 1 : 4").eval(0) shouldBe 4
    p("n ? 1 : 4").eval(2) shouldBe 1
    p("n ? 1 : 4").eval(-1) shouldBe 1
    p("0 ? 1 : 0 ? 2 : 0 ? 3 : 4").eval(0) shouldBe 4
    p("(n ? 0 : 1) ? 2 : 3").eval(0) shouldBe 2
    p("(n ? 0 : 1) ? 2 : 3").eval(1) shouldBe 3
  }

  it should "discard invalid expressions" in {
    def err(f: => Expression): Unit = an [IllegalArgumentException] shouldBe thrownBy(f)

    err { p("") }
    err { p("+") }
    err { p("1 +") }
    err { p("1 1") }
    err { p("n n") }
    err { p("n 1") }
    err { p("1 + + + 4") }
    err { p("(1") }
    err { p(")") }
    err { p(")(") }
    err { p("n ? 1") }
    err { p("n : 1") }
    err { p("n : 1 ? 2") }
    err { p("n ? 1 : n ? 2") }
    err { p("n;1")}
  }

  it should "evaluate real-world examples" in {
    // Russian:
    val r = p("(n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2);")

    r.eval(0) shouldBe 2
    r.eval(1) shouldBe 0
    r.eval(2) shouldBe 1
    r.eval(3) shouldBe 1
    r.eval(5) shouldBe 2
    r.eval(9) shouldBe 2
    r.eval(11) shouldBe 2
    r.eval(13) shouldBe 2
    r.eval(21) shouldBe 0
    r.eval(23) shouldBe 1
    r.eval(25) shouldBe 2
    r.eval(50) shouldBe 2
    r.eval(51) shouldBe 0
    r.eval(111) shouldBe 2
  }

  it should "compile expressions" in {
    p("1").scalaExpression shouldBe "1L"
    p("n").scalaExpression shouldBe "arg"
    p("2+2").scalaExpression shouldBe "(2L)+(2L)"
    p("(n==2)").scalaExpression shouldBe "if ((arg)==(2L)) 1 else 0"
    p("(n==2)+1").scalaExpression shouldBe "(if ((arg)==(2L)) 1 else 0)+(1L)"
    p("n==2?3:10").scalaExpression shouldBe "if ((arg)==(2L)) (3L) else (10L)"
  }
}
