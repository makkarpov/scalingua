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

object Expression {
  case class Constant(v: Long) extends Expression {
    override def eval(n: Long): Long = v

    override def boolResult: Boolean = (v == 0) || (v == 1)
    override def strictResult = false

    override protected def writeBool(sb: StringBuilder) = sb.append(v != 0)
    override protected def writeLong(sb: StringBuilder) = sb.append(v) += 'L'
  }

  case object Variable extends Expression {
    override def eval(n: Long): Long = n

    override def boolResult: Boolean = false
    override def strictResult: Boolean = true

    override protected def writeLong(sb: StringBuilder) = sb ++= "arg"
  }

  case class UnaryOp(op: String, arg: Expression) extends Expression {
    override def eval(n: Long): Long = {
      val v = arg.eval(n)
      op match {
        case "+" => v
        case "-" => -v
        case "~" => ~v
        case "!" => if (v == 0) 1 else 0
      }
    }

    override def boolResult: Boolean = op == "!"
    override def strictResult: Boolean = true

    override protected def writeBool(sb: StringBuilder): StringBuilder =
      if (arg.boolResult) arg.asBool(sb ++= "!(") += ')'
      else arg.asBool(sb += '(') ++= ") == 0"

    override protected def writeLong(sb: StringBuilder): StringBuilder =
      arg.asLong(sb ++= op += '(') += ')'
  }

  case class BinaryOp(op: String, left: Expression, right: Expression) extends Expression {
    @inline private def long(b: Boolean): Long = if (b) 1 else 0

    override def eval(n: Long): Long = op match {
      case "+" => left.eval(n) + right.eval(n)
      case "-" => left.eval(n) - right.eval(n)
      case "*" => left.eval(n) * right.eval(n)
      case "/" => left.eval(n) / right.eval(n)
      case "%" => left.eval(n) % right.eval(n)

      case "<<" => left.eval(n) << right.eval(n)
      case ">>" => left.eval(n) >> right.eval(n)

      case "<=" => long(left.eval(n) <= right.eval(n))
      case "<"  => long(left.eval(n) <  right.eval(n))
      case ">=" => long(left.eval(n) >= right.eval(n))
      case ">"  => long(left.eval(n) >  right.eval(n))
      case "==" => long(left.eval(n) == right.eval(n))
      case "!=" => long(left.eval(n) != right.eval(n))

      case "&"  => left.eval(n) & right.eval(n)
      case "|"  => left.eval(n) | right.eval(n)
      case "^"  => left.eval(n) ^ right.eval(n)

      case "&&" => long((left.eval(n) != 0) && (right.eval(n) != 0))
      case "||" => long((left.eval(n) != 0) || (right.eval(n) != 0))
    }

    override def boolResult: Boolean = op match {
      case ">" | "<" | ">=" | "<=" | "!=" | "==" => true
      case "&&" | "||" => true
      case "&" | "|" | "^" => left.boolResult && right.boolResult
      case _ => false
    }

    override def strictResult: Boolean = true

    override protected def writeBool(sb: StringBuilder): StringBuilder = op match {
      case ">" | "<" | ">=" | "<=" | "!=" | "==" =>
        if (left.boolResult && right.boolResult)
          right.asBool(left.asBool(sb += '(') += ')' ++= op += '(') += ')'
        else
          right.asLong(left.asLong(sb += '(') += ')' ++= op += '(') += ')'
      case _ => right.asBool(left.asBool(sb += '(') += ')' ++= op += '(') += ')'
    }


    override protected def writeLong(sb: StringBuilder): StringBuilder =
      right.asLong(left.asLong(sb += '(') += ')' ++= op += '(') += ')'
  }

  case class TernaryOp(cond: Expression, ifTrue: Expression, ifFalse: Expression) extends Expression {
    override def eval(n: Long): Long = if (cond.eval(n) != 0) ifTrue.eval(n) else ifFalse.eval(n)

    override def boolResult: Boolean = ifTrue.boolResult && ifFalse.boolResult
    override def strictResult: Boolean = ifTrue.strictResult || ifFalse.strictResult

    override protected def writeLong(sb: StringBuilder): StringBuilder = writeBool(sb) // type-invariant
    override protected def writeBool(sb: StringBuilder): StringBuilder = {
      sb ++= "if ("
      cond.asBool(sb)
      sb ++= ") ("
      if (boolResult) ifTrue.asBool(sb)
      else ifTrue.asLong(sb)
      sb ++= ") else ("
      if (boolResult) ifFalse.asBool(sb)
      else ifFalse.asLong(sb)
      sb += ')'
    }
  }
}

sealed trait Expression {
  def boolResult: Boolean
  def strictResult: Boolean

  protected def writeBool(sb: StringBuilder): StringBuilder = sb
  protected def writeLong(sb: StringBuilder): StringBuilder = sb

  def asBool(sb: StringBuilder): StringBuilder =
    if (boolResult || !strictResult) writeBool(sb)
    else {
      sb += '('
      writeLong(sb)
      sb ++= ") != 0"
    }

  def asLong(sb: StringBuilder): StringBuilder =
    if (!boolResult || !strictResult) writeLong(sb)
    else {
      sb ++= "if ("
      writeBool(sb)
      sb ++= ") 1 else 0"
    }

  def scalaExpression = asLong(new StringBuilder).result()
  override def toString: String = scalaExpression

  def eval(n: Long): Long
}
