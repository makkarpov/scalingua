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

/**
  * A very simple and lightweight parser for C-like expressions
  */
object Parser {
  /** Operators with highest precedence */
  val UnaryOperators = Set( "+", "-", "~", "!" )

  /** Binary operators in format "op" -> (precedence, right assoc) */
  val BinaryOperators = Map(
    "*" -> (1, false), "/" -> (1, false), "%" -> (1, false),
    "-" -> (2, false), "+" -> (2, false),
    "<<" -> (3, false), ">>" -> (3, false),
    "<=" -> (4, false), "<" -> (4, false), ">=" -> (4, false), ">" -> (4, false),
    "==" -> (5, false), "!=" -> (5, false),
    "&" -> (6, false),
    "^" -> (7, false),
    "|" -> (8, false),
    "&&" -> (9, false),
    "||" -> (10, false),
    "?" -> (11, true), ":" -> (11, true)
  )

  private val Operators = (UnaryOperators ++ BinaryOperators.keys).toSet
  private val OperatorChars = Operators.flatten

  sealed trait Token
  sealed trait OperatorLike
  sealed trait ValueLike

  case class NumberToken(num: Long) extends Token with ValueLike
  case object NVariableToken extends Token with ValueLike
  case class OperatorToken(op: String) extends Token with OperatorLike
  case class ParenthesesToken(open: Boolean) extends Token
  case class ExpressionToken(expr: Expression) extends Token with ValueLike

  def apply(s: String): Expression = parse(tokenize(s))

  def tokenize(s: String): Seq[Token] = {
    val chars = s.iterator.buffered
    val tokens = Seq.newBuilder[Token]

    for (chr <- chars) chr match {
      case 'n' => tokens += NVariableToken
      case c if c.isDigit =>
        val number = new StringBuilder
        number += c
        while (chars.hasNext && chars.head.isDigit)
          number += chars.next()
        tokens += NumberToken(number.result().toLong)
      case c @ ('(' | ')') => tokens += ParenthesesToken(c == '(')
      case c if OperatorChars.contains(c) =>
        val op = new StringBuilder
        op += c
        while (chars.hasNext && OperatorChars.contains(chars.head))
          op += chars.next()
        tokens += OperatorToken(op.result())
      case c if c.isWhitespace => /* .. */
      case ';' if !chars.hasNext => /* .. */
      case x => throw new IllegalArgumentException(s"Illegal character $x")
    }

    tokens.result()
  }

  def parse(rawTokens: Seq[Token]): Expression = {
    // flatten parentheses
    val noParens = {
      val bld = Seq.newBuilder[Token]
      var buf = Seq.newBuilder[Token]
      var parDepth = 0

      for (t <- rawTokens) t match {
        case ParenthesesToken(true) =>
          if (parDepth > 0) buf += t
          parDepth += 1

        case ParenthesesToken(false) =>
          parDepth -= 1

          if (parDepth < 0)
            throw new IllegalArgumentException("Unbalanced parentheses")

          if (parDepth == 0) {
            bld += ExpressionToken(parse(buf.result()))
            buf = Seq.newBuilder
          } else buf += t

        case _ => (if (parDepth > 0) buf else bld) += t
      }

      if (parDepth > 0)
        throw new IllegalArgumentException("Unbalanced parentheses")

      bld.result()
    }

    // now operators and operands are interleaved with each other, so parse unary:
    val unaryOk = {
      val bld = Seq.newBuilder[Token]
      var nextUnary = true
      val tokens = noParens.iterator

      for (t <- tokens) t match {
        case OperatorToken(sym) if nextUnary =>
          if (!UnaryOperators.contains(sym))
            throw new IllegalArgumentException(s"Undefined unary operator: $sym")

          if (!tokens.hasNext)
            throw new IllegalArgumentException("Expected a value, got end-of-input")

          val data = tokens.next() match {
            case v: ValueLike => parse(v :: Nil)
            case _ => throw new IllegalArgumentException("Value expected, but got an operator")
          }

          bld += ExpressionToken(Expression.UnaryOp(sym, data))
          nextUnary = false

        case OperatorToken(_) =>
          bld += t
          nextUnary = true

        case _: ValueLike if !nextUnary =>
          throw new IllegalArgumentException("Operator expected, but got a value")

        case _: ValueLike =>
          bld += t
          nextUnary = false
      }

      if (nextUnary)
        throw new IllegalArgumentException("Expected a value, got end-of-input")

      bld.result()
    }

    // now token sequence looks like this: val op val op val op val op ...
    // find an operator(s) with lowest precedence and apply them in associative order:

    val (lowestPrecedence, rightAssoc) = {
      var ret = Int.MinValue
      var right = false

      for (t <- unaryOk) t match {
        case _: ValueLike =>
        case OperatorToken(sym) => BinaryOperators.get(sym) match {
          case Some((prec, ra)) =>
            if (ret < prec) {
              ret = prec
              right = ra
            }

          case None =>
            throw new IllegalArgumentException(s"Undefined binary operator: $sym")
        }

        case _ => throw new RuntimeException(s"Bad token at precedence check: $t") // should never reach this, just to suppress warning
      }

      (ret, right)
    }

    if (lowestPrecedence == Int.MinValue) {
      // there is no operators. Either it's empty input or single token

      return unaryOk match {
        case Seq(ExpressionToken(e)) => e
        case Seq(NVariableToken) => Expression.Variable
        case Seq(NumberToken(n)) => Expression.Constant(n)

        case Seq() => throw new IllegalArgumentException("Got empty input")
        case x => throw new IllegalArgumentException(s"Illegal token sequence: $x")
      }
    }

    assert(unaryOk.head.isInstanceOf[ValueLike])
    assert(unaryOk.last.isInstanceOf[ValueLike])

    // compute all operators except low-precedence ones
    var flat = {
      var ret = Seq.newBuilder[Token]
      var buf = Seq.newBuilder[Token]

      for (t <- unaryOk) t match {
        case OperatorToken(sym) if BinaryOperators(sym)._1 == lowestPrecedence =>
          ret += ExpressionToken(parse(buf.result()))
          ret += t
          buf = Seq.newBuilder

        case _ => buf += t
      }

      ret += ExpressionToken(parse(buf.result()))

      ret.result()
    }

    // fold it!

    if (lowestPrecedence == BinaryOperators("?")._1) {
      // ternary operator, right associative
      // the token sequence would look like this:
      //  a ? b : c ? d : e ? f : g,
      // with interleaving ?:'s. It should be parsed as
      //  if (a) b else { if (c) d else { if (e) f else g } }

      // validate
      if (flat.size < 5)
        throw new IllegalArgumentException("Insufficient ternary operator parts")

      if (((flat.size - 1) / 2) % 2 != 0)
        throw new IllegalArgumentException("Odd number of ternary operator parts")

      def ternary(i: Int): Expression = {
        flat(i + 1) match {
          case OperatorToken("?") => /* OK */
          case OperatorToken(x) => throw new IllegalArgumentException(s"Illegal operator '$x', '?' expected")
          case _ => throw new RuntimeException(s"Bad token at ternary operator: ${flat(i + 1)}") // should never reach this, just to suppress warning
        }

        flat(i + 3) match {
          case OperatorToken(":") => /* OK */
          case OperatorToken(x) => throw new IllegalArgumentException(s"Illegal operator '$x', ':' expected")
          case _ => throw new RuntimeException(s"Bad token at ternary operator: ${flat(i + 1)}") // should never reach this, just to suppress warning
        }

        Expression.TernaryOp(
          cond = flat(i).asInstanceOf[ExpressionToken].expr,
          ifTrue = flat(i + 2).asInstanceOf[ExpressionToken].expr,
          ifFalse = if (i + 4 == flat.size - 1) flat(i + 4).asInstanceOf[ExpressionToken].expr
          else ternary(i + 4)
        )
      }

      ternary(0)
    } else {
      if (rightAssoc)
        flat = flat.reverse

      val tokens = flat.iterator
      var ExpressionToken(current) = tokens.next()

      while (tokens.hasNext) {
        val OperatorToken(op) = tokens.next()
        val ExpressionToken(arg) = tokens.next()

        current =
          if (rightAssoc) Expression.BinaryOp(op, arg, current)
          else Expression.BinaryOp(op, current, arg)
      }

      current
    }
  }
}
