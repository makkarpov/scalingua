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

import ru.makkarpov.scalingua.extract.MessageExtractor

import scala.reflect.macros.whitebox

object Compat {
  type Context = whitebox.Context

  // Everything becomes better with quasiquotes.

  def showCode(c: Context)(e: c.Tree): String = c.universe.showCode(e)

  def tupleLiteral[T](c: Context)(e: c.Expr[(String, T)]): (String, c.Expr[T]) = {
    import c.universe._

    val (aTree, bTree) = e.tree match {
      case q"scala.this.Predef.ArrowAssoc[$_]($a).->[$_]($b)" => (a, b)
      case q"($a, $b)" => (a, b)
      case _ =>
        c.abort(c.enclosingPosition, s"Expected tuple definition `x -> y` or `(x, y)`, got instead ${c.universe.showCode(e.tree)}")
    }

    val keyLiteral = aTree match {
      case Literal(Constant(x: String)) => x
      case _ => c.abort(c.enclosingPosition, s"Expected string literal as first entry of tuple, got ${c.universe.showCode(e.tree)} instead")
    }

    (keyLiteral, c.Expr[T](bTree))
  }

  def generateSingular[T: c.WeakTypeTag](c: Context)(ctx: Option[String], str: String, args: Map[String, c.Tree])
                                        (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    MessageExtractor.singular(c)(ctx, str)

    import c.universe._

    val tr = c.Expr[String](ctx match {
      case Some(s) => q"${lang.tree}.singular($s, $str)"
      case None => q"${lang.tree}.singular($str)"
    })

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  def generatePlural[T: c.WeakTypeTag](c: Context)(ctx: Option[String], str: String, strPlural: String, n: c.Expr[Long],
                 args: Map[String, c.Tree])(lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    MessageExtractor.plural(c)(ctx, str, strPlural)

    import c.universe._

    val tr = c.Expr[String](ctx match {
      case Some(s) => q"${lang.tree}.plural($s, $str, $strPlural, ${n.tree})"
      case None => q"${lang.tree}.plural($str, $strPlural, ${n.tree})"
    })

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  private def generateInterpolation[T: c.WeakTypeTag](c: Context)(str: c.Expr[String], args: Map[String, c.Tree],
                                                              outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    import c.universe._

    c.Expr[T](if (args.nonEmpty) {
      val argList = args.map { case (k, v) => q"$k -> $v" }
      q"_root_.ru.makkarpov.scalingua.StringUtils.interpolate[${weakTypeOf[T]}](${str.tree}, ..$argList)(${outputFormat.tree})"
    } else q"${outputFormat.tree}.convert(${str.tree})")
  }
}
