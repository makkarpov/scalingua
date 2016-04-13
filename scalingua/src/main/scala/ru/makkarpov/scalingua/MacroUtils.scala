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

package ru.makkarpov.scalingua

import Compat._
import ru.makkarpov.scalingua.extract.MessageExtractor

protected object MacroUtils {
  def stringLiteral[T](c: Context)(e: c.Tree): String = {
    import c.universe._

    e match {
      case Literal(Constant(s: String)) => s

      case q"scala.this.Predef.augmentString($str).stripMargin" =>
        str match {
          case Literal(Constant(s: String)) => s.stripMargin.trim
          case _ => c.abort(c.enclosingPosition, s"Expected string literal, got instead ${prettyPrint(c)(str)}")
        }

      case q"scala.this.Predef.augmentString($str).stripMargin($ch)" =>
        (str, ch) match {
          case (Literal(Constant(s: String)), Literal(Constant(c: Char))) => s.stripMargin(c).trim
          case (Literal(Constant(s: String)), _) =>
            c.abort(c.enclosingPosition, s"Expected character literal, got instead ${prettyPrint(c)(ch)}")
          case _ => c.abort(c.enclosingPosition, s"Expected string literal, got instead ${prettyPrint(c)(str)}")
        }

      case _ =>
        c.abort(c.enclosingPosition, s"Expected string literal or multi-line string, got instead ${prettyPrint(c)(e)}")
    }
  }

  def tupleLiteral[T](c: Context)(e: c.Expr[(String, T)]): (String, c.Expr[T]) = {
    import c.universe._

    val (aTree, bTree) = e.tree match {
      case q"scala.this.Predef.ArrowAssoc[$aType]($a).->[$bType]($b)" => (a, b) // 2.11
      case q"scala.this.Predef.any2ArrowAssoc[$aType]($a).->[$bType]($b)" => (a, b) // 2.10
      case q"($a, $b)" => (a, b)
      case _ =>
        c.abort(c.enclosingPosition, s"Expected tuple definition `x -> y` or `(x, y)`, got instead ${prettyPrint(c)(e.tree)}")
    }

    val keyLiteral = aTree match {
      case Literal(Constant(x: String)) => x
      case _ => c.abort(c.enclosingPosition, s"Expected string literal as first entry of tuple, got ${prettyPrint(c)(e.tree)} instead")
    }

    (keyLiteral, c.Expr[T](bTree))
  }

  def generateSingular[T: c.WeakTypeTag]
    (c: Context)
    (ctx: Option[String], str: String, args: Map[String, c.Tree])
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    MessageExtractor.singular(c)(ctx, str)

    import c.universe._

    val tr = c.Expr[String](ctx match {
      case Some(s) => q"${lang.tree}.singular($s, $str)"
      case None => q"${lang.tree}.singular($str)"
    })

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  def generatePlural[T: c.WeakTypeTag]
    (c: Context)
    (ctx: Option[String], str: String, strPlural: String, n: c.Expr[Long], args: Map[String, c.Tree])
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    MessageExtractor.plural(c)(ctx, str, strPlural)

    import c.universe._

    val tr = c.Expr[String](ctx match {
      case Some(s) => q"${lang.tree}.plural($s, $str, $strPlural, ${n.tree})"
      case None => q"${lang.tree}.plural($str, $strPlural, ${n.tree})"
    })

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  private def generateInterpolation[T: c.WeakTypeTag]
    (c: Context)
    (str: c.Expr[String], args: Map[String, c.Tree], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    import c.universe._

    c.Expr[T](if (args.nonEmpty) {
      val argList = args.map { case (k, v) => q"$k -> $v" }
      q"_root_.ru.makkarpov.scalingua.StringUtils.interpolate[${weakTypeOf[T]}](${str.tree}, ..$argList)(${outputFormat.tree})"
    } else q"${outputFormat.tree}.convert(${str.tree})")
  }
}
