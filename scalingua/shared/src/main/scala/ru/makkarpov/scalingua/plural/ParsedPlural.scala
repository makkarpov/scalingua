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

package ru.makkarpov.scalingua.plural

import ru.makkarpov.scalingua.PluralFunction

object ParsedPlural {
  val English = new ParsedPlural {
    override def expr = Parser("!(n == 1)")
    override def numPlurals: Int = 2
    override def plural(n: Long): Int = if (n != 1) 1 else 0
  }

  private val header = "^\\s*nplurals=(\\d+); plural=(.*);?$".r

  def fromHeader(s: String): ParsedPlural = s match {
    case header(num, func) => new ParsedPlural {
      val expr = Parser(func)
      override def numPlurals: Int = num.toInt
      override def plural(n: Long): Int = expr.eval(n).toInt
    }

    case _ => throw new IllegalArgumentException(s"Bad plurals string format: $s")
  }
}

trait ParsedPlural extends PluralFunction {
  def expr: Expression
}
