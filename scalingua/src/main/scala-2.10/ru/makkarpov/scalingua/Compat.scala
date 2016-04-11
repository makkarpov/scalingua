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

object Compat {
  type Context = scala.reflect.macros.Context

  def showCode(c: Context)(e: c.Tree): String = c.universe.show(e)

  def tupleLiteral[T](c: Context)(e: c.Expr[(String, T)]): (String, c.Expr[T]) = {
    import c.universe._

    val (aTree, bTree) = e.tree match {
      // matches AST for a -> b
      case Apply(TypeApply(
        Select(
          Apply(TypeApply(
            // scala.Predef.arrowAssoc (no extractor for type names. so match them manually)
            Select(Select(This(tnScala: TypeName), tnPredef: Name), tnArrAss: TermName), List(TypeTree())
          ), List(a)),
          // $minus$greater
          tnMg: TermName
        ), List(TypeTree())),  List(b)
      ) if (tnScala.decodedName.toString == "scala") && (tnPredef.decodedName.toString == "Predef") &&
        (tnArrAss.decodedName.toString == "any2ArrowAssoc") && (tnMg.decodedName.toString == "->") => (a, b)

      // matches AST for (a, b)
      case Apply(
        TypeApply(
          Select(Select(Ident(tnScala), tnTuple), tnApply),
          List(TypeTree(), TypeTree())
        ), List(a, b)
      ) if (tnScala.decodedName.toString == "scala") && (tnTuple.decodedName.toString == "Tuple2") &&
        (tnApply.decodedName.toString == "apply") => (a, b)

      case _ => c.abort(c.enclosingPosition, s"Expected tuple definition `x -> y` or `(x, y)`, got instead ${showCode(c)(e.tree)}")
    }

    (aTree match {
      case Literal(Constant(x: String)) => x
      case _ => c.abort(c.enclosingPosition, s"Expected string literal as first entry of tuple, got ${showCode(c)(aTree)} instead")
    }, c.Expr[T](bTree))
  }

  def generateSingular[T: c.WeakTypeTag](c: Context)(ctx: Option[String], str: String, args: Map[String, c.Tree])
                    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    MessageExtractor.singular(c)(ctx, str)

    import c.universe._

    val strLit = c.literal(str)
    val tr = ctx match {
      case Some(s) =>
        val ctxLit = c.literal(s)
        reify { lang.splice.singular(ctxLit.splice, strLit.splice) }
      case None => reify { lang.splice.singular(strLit.splice) }
    }

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  def generatePlural[T: c.WeakTypeTag](c: Context)(ctx: Option[String], str: String, strPlural: String, n: c.Expr[Long],
               args: Map[String, c.Tree])(lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    MessageExtractor.plural(c)(ctx, str, strPlural)

    import c.universe._

    val strLit = c.literal(str)
    val strPluralLit = c.literal(strPlural)

    val tr = ctx match {
      case Some(s) =>
        val ctxLit = c.literal(s)
        reify { lang.splice.plural(ctxLit.splice, strLit.splice, strPluralLit.splice, n.splice) }

      case None => reify { lang.splice.plural(strLit.splice, strPluralLit.splice, n.splice) }
    }

    generateInterpolation[T](c)(tr, args, outputFormat).asInstanceOf[c.Expr[T]]
  }

  private def generateInterpolation[T: c.WeakTypeTag](c: Context)(str: c.Expr[String], args: Map[String, c.Tree],
                                                      outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] = {

    import c.universe._

    if (args.nonEmpty) {
      val argList = flipSeq(c)(args.map {
        case (name, value) =>
          val nlit = c.literal(name)
          val vlit = c.Expr[Any](value)

          reify { nlit.splice -> vlit.splice }
      }.toSeq)

      reify {
        StringUtils.interpolate[T](
          str.splice,
          argList.splice:_*
        )(outputFormat.splice)
      }
    } else {
      reify {
        outputFormat.splice.convert(str.splice)
      }
    }
  }

  private def flipSeq[T](c: Context)(t: Seq[c.Expr[T]]): c.Expr[Seq[T]] = {
    import c.universe._

    c.Expr[Seq[T]](Apply(
      Select(reify(Seq).tree, newTermName("apply")),
      t.map(_.tree).toList
    ))
  }
}
