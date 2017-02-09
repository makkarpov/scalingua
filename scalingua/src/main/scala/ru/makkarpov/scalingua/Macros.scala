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
import ru.makkarpov.scalingua.plural.Suffix
import ru.makkarpov.scalingua.InsertableIterator._

object Macros {
  // All macros variants: (lazy, eager) x (singular, plural) x (interpolation, ctx, non ctx)), 12 total

  // Interpolators:

  def interpolate[T: c.WeakTypeTag](c: Context)
    (args: c.Expr[Any]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    import c.universe._
    val (msg, argsT) = interpolator(c)(args.map(_.tree))
    c.Expr[T](generate[T](c)(None, q"$msg", None, argsT)(Some(lang.tree), outputFormat.tree))
  }

  def lazyInterpolate[T: c.WeakTypeTag](c: Context)
    (args: c.Expr[Any]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
  {
    import c.universe._
    val (msg, argsT) = interpolator(c)(args.map(_.tree))
    c.Expr[LValue[T]](generate[T](c)(None, q"$msg", None, argsT)(None, outputFormat.tree))
  }

  def pluralInterpolate[T: c.WeakTypeTag](c: Context)
    (args: c.Expr[Any]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    import c.universe._
    val (msg, msgP, argsT, nVar) = pluralInterpolator(c)(args.map(_.tree))
    c.Expr[T](generate[T](c)(None, q"$msg", Some((q"$msgP", nVar, false)), argsT)(Some(lang.tree), outputFormat.tree))
  }


  def lazyPluralInterpolate[T: c.WeakTypeTag](c: Context)
    (args: c.Expr[Any]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
  {
    import c.universe._
    val (msg, msgP, argsT, nVar) = pluralInterpolator(c)(args.map(_.tree))
    c.Expr[LValue[T]](generate[T](c)(None, q"$msg", Some((q"$msgP", nVar, false)), argsT)(None, outputFormat.tree))
  }

  // Singular:

  def singular[T: c.WeakTypeTag](c: Context)
    (msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
    c.Expr[T](generate[T](c)(None, msg.tree, None, args.map(_.tree))(Some(lang.tree), outputFormat.tree))

  def lazySingular[T: c.WeakTypeTag](c: Context)
    (msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
    c.Expr[LValue[T]](generate[T](c)(None, msg.tree, None, args.map(_.tree))(None, outputFormat.tree))

  def singularCtx[T: c.WeakTypeTag](c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
    c.Expr[T](generate[T](c)(Some(ctx.tree), msg.tree, None, args.map(_.tree))(Some(lang.tree), outputFormat.tree))

  def lazySingularCtx[T: c.WeakTypeTag](c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
    c.Expr[LValue[T]](generate[T](c)(Some(ctx.tree), msg.tree, None, args.map(_.tree))(None, outputFormat.tree))

  // Plural:

  def plural[T: c.WeakTypeTag](c: Context)
    (msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
    c.Expr[T](generate[T](c)(None, msg.tree, Some((msgPlural.tree, n.tree, true)), args.map(_.tree))
                            (Some(lang.tree), outputFormat.tree))

  def lazyPlural[T: c.WeakTypeTag](c: Context)
    (msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
    c.Expr[LValue[T]](generate[T](c)(None, msg.tree, Some((msgPlural.tree, n.tree, true)), args.map(_.tree))
                                    (None, outputFormat.tree))

  def pluralCtx[T: c.WeakTypeTag](c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
    c.Expr[T](generate[T](c)(Some(ctx.tree), msg.tree, Some((msgPlural.tree, n.tree, true)), args.map(_.tree))
                            (Some(lang.tree), outputFormat.tree))

  def lazyPluralCtx[T: c.WeakTypeTag](c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (outputFormat: c.Expr[OutputFormat[T]]): c.Expr[LValue[T]] =
    c.Expr[LValue[T]](generate[T](c)(Some(ctx.tree), msg.tree, Some((msgPlural.tree, n.tree, true)), args.map(_.tree))
                                    (None, outputFormat.tree))

  // Macro internals:

  /**
    * A generic macro that extracts interpolation string and set of interpolation
    * variables from string interpolator invocation.
    *
    * @param c Macro context
    * @param args Arguments of interpolator
    * @return (Extracted string, extracted variables)
    */
  private def interpolator(c: Context)(args: Seq[c.Tree]): (String, Seq[c.Tree]) = {
    import c.universe._

    // Extract raw interpolation parts
    val parts = c.prefix.tree match {
      case Apply(_, List(Apply(_, rawParts))) =>
        rawParts.map(stringLiteral(c)(_)).map(StringContext.treatEscapes)

      case _ =>
        c.abort(c.enclosingPosition, s"failed to match prefix, got ${prettyPrint(c)(c.prefix.tree)}")
    }

    interpolationString(c)(parts, args)
  }

  /**
    * A macro function that extracts singular and plural strings, arguments and `n` variable from plural interpolation.
    *
    * E.g.: `I have $n fox${S.ex}` ->
    *  * Singular string: "I have %(n) fox"
    *  * Plural string: "I have %(n) foxes"
    *  * Arguments: <| "n" -> n |>
    *  * N variable: <| n |>
    *
    * @param c Macro context
    * @param args Interpolation arguments
    * @return
    */
  private def pluralInterpolator(c: Context)(args: Seq[c.Tree]): (String, String, Seq[c.Tree], c.Tree) = {
    import c.universe._

    val parts = c.prefix.tree match {
      case Apply(_, List(Apply(_, rawParts))) =>
        rawParts.map(stringLiteral(c)(_)).map(StringContext.treatEscapes)

      case _ =>
        c.abort(c.enclosingPosition, s"failed to match prefix, got ${prettyPrint(c)(c.prefix.tree)}")
    }

    assert(parts.size == args.size + 1)

    def nVarHint(expr: c.Tree): Option[c.Tree] = expr match {
      case q"$prefix.int2MacroExtension($arg).nVar" => Some(arg)
      case q"$prefix.long2MacroExtension($arg).nVar" => Some(arg)
      case _ => None
    }

    // Find a variable that represents plural number and strip `.nVar`s, if any.
    val (filteredArgs, nVar) = {
      val intVars = args.indices.filter { i =>
        val tpe = typecheck(c)(args(i)).tpe
        (tpe <:< typeOf[Int]) || (tpe <:< typeOf[Long])
      }

      val nVars = args.indices.filter(i => nVarHint(args(i)).isDefined)

      val chosenN = (intVars, nVars) match {
        case (_, Seq(i)) => i
        case (_, Seq(_, _*)) => c.abort(c.enclosingPosition, "multiple `.nVar` annotations present")
        case (Seq(i), _) => i
        case (Seq(), Seq()) => c.abort(c.enclosingPosition, "no integer variable is present - provide at least one as plural number")
        case _ => c.abort(c.enclosingPosition, "multiple integer variables present. Annotate one that represents a plural number with `x.nVar`")
      }

      val fArgs = args.map(x => nVarHint(x).getOrElse(x))
      (fArgs, fArgs(chosenN))
    }

    // Merge parts separated by plural suffix - e.g. "fox"$es"" becomes "fox" and "foxes".
    val (partsSingular, partsPlural, finalArgs) = {
      val itS = parts.iterator.insertable
      val itP = parts.iterator.insertable

      val retS = Seq.newBuilder[String]
      val retP = Seq.newBuilder[String]
      val retA = Seq.newBuilder[c.Tree]

      for {
        arg <- args
        tpe = typecheck(c)(arg).tpe
      } if (tpe <:< weakTypeOf[Suffix.Generic]) {
        arg match {
          case q"$prefix.string2SuffixExtension($sing).&>($plur)" =>
            itS.unnext(itS.next() + stringLiteral(c)(sing) + itS.next())
            itP.unnext(itP.next() + stringLiteral(c)(plur) + itP.next())

          case _ =>
            c.abort(c.enclosingPosition, s"expression of type `Suffix.Generic` should have `a &> b` form, got instead `${prettyPrint(c)(arg)}`")
        }
      } else if (tpe <:< weakTypeOf[Suffix]) {
        val rawSuffix =
          if (tpe <:< weakTypeOf[Suffix.S]) "s"
          else if (tpe <:< weakTypeOf[Suffix.ES]) "es"
          else c.abort(c.enclosingPosition, s"unknown suffix type: $tpe")

        val suffix =
          if (itS.head.nonEmpty && Character.isUpperCase(itS.head.last)) rawSuffix.toUpperCase
          else rawSuffix

        itS.unnext(itS.next() + itS.next())
        itP.unnext(itP.next() + suffix + itP.next())
      } else {
        retS += itS.next()
        retP += itP.next()
        retA += arg
      }

      // One part should remain:
      retS += itS.next()
      retP += itP.next()

      (retS.result(), retP.result(), retA.result())
    }

    // Build interpolation strings by parts
    val (sStr, tArgs) = interpolationString(c)(partsSingular, finalArgs)
    // These string are guaranteed to have the same structure, so we can ignore second args:
    val (pStr, _) = interpolationString(c)(partsPlural, finalArgs)

    (sStr, pStr, tArgs, nVar)
  }

  /**
    * A generic function to generate interpolation results. Other macros do nothing but call it.
    *
    * @param c Macro context
    * @param ctxTree Optional tree with context argument
    * @param msgTree Message argument
    * @param pluralTree Optional with (plural message, n, insert "n" arg) arguments
    * @param argsTree Supplied args as a trees
    * @param lang Language tree that if present means instant evaluation
    * @param outputFormat Tree representing `OutputFormat[T]` instance
    * @return Tree representing an instance of `T` if language was present, or `LValue[T]` if
    *         language was absent.
    */
  private def generate[T: c.WeakTypeTag](c: Context)
    (ctxTree: Option[c.Tree], msgTree: c.Tree, pluralTree: Option[(c.Tree, c.Tree, Boolean)], argsTree: Seq[c.Tree])
    (lang: Option[c.Tree], outputFormat: c.Tree): c.Tree =
  {
    import c.universe._

    // Extract literals:
    val ctx = ctxTree.map(stringLiteral(c))
    val msg = stringLiteral(c)(msgTree)
    val plural = pluralTree.map { case (s, n, i) => (stringLiteral(c)(s), n, i) }
    val args = argsTree.map(tupleLiteral(c)(_)) ++ (plural match {
      case Some((_, n, true)) => Seq("n" -> n)
      case _ => Nil
    })

    // Call message extractor:
    plural match {
      case None => MessageExtractor.singular(c)(ctx, msg)
      case Some((pl, _, _)) => MessageExtractor.plural(c)(ctx, msg, pl)
    }

    // Verify variables consistency:
    def verifyVariables(s: String): Unit = {
      val varsArg = args.map(_._1).toSet
      val varsStr = StringUtils.extractVariables(s).toSet

      for (v <- (varsArg diff varsStr) ++ (varsStr diff varsArg))
        if (varsArg.contains(v))
          c.abort(c.enclosingPosition, s"variable `$v` is not present in interpolation string")
        else
          c.abort(c.enclosingPosition, s"variable `$v` is not present at arguments section")
    }

    for ((v, xs) <- args.groupBy(_._1) if xs.length > 1)
      c.abort(c.enclosingPosition, s"duplicate variable `$v`")

    verifyVariables(msg)
    for ((pl, _, _) <- plural)
      verifyVariables(pl)

    /**
      * Given a language tree `lng`, creates a tree that will translate given message.
      */
    def translate(lng: c.Tree): c.Tree = {
      val str = plural match {
        case None => q"$lng.singular(..${ctx.toSeq}, $msg)"
        case Some((pl, n, _)) => q"$lng.plural(..${ctx.toSeq}, $msg, $pl, $n)"
      }

      if (args.isEmpty)
        q"$outputFormat.convert($str)"
      else {
        val argsT = processArgs(c)(args, lng)
        q"_root_.ru.makkarpov.scalingua.StringUtils.interpolate[${weakTypeOf[T]}]($str, ..$argsT)"
      }
    }

    lang match {
      case Some(lng) => translate(lng)
      case None =>
        val name = termName(c)("lng")
        q"""
          new _root_.ru.makkarpov.scalingua.LValue(
            ($name: _root_.ru.makkarpov.scalingua.Language) => ${translate(q"$name")}
          )
        """
    }
  }

  /**
    * Convert name/value pairs to a sequence of tuples and expands specific arguments.
    * @param c
    * @param args
    * @return
    */
  private def processArgs(c: Context)(args: Seq[(String, c.Tree)], lang: c.Tree): Seq[c.Tree] = args.map {
    case (k, v) =>
      import c.universe._

      val tpe = typecheck(c)(v).tpe
      val xv =
        if (tpe <:< weakTypeOf[LValue[_]]) q"$v($lang)"
        else v

      q"$k -> $xv"
  }

  /**
    * Given the parts of interpolation string and trees of interpolation arguments, this function tries to
    * guess final string with variable names like "Hello, %(name)!"
    *
    * @param c Macro context
    * @param parts Interpolation string parts
    * @param args Interpolation variables
    * @return Final string and trees of arguments to `StringUtils.interpolate` (in format of `a -> b`)
    */
  private def interpolationString(c: Context)(parts: Seq[String], args: Seq[c.Tree]): (String, Seq[c.Tree]) = {
    import c.universe._

    assert(parts.size == args.size + 1)

    val inferredNames = args.map {
      case Ident(name: TermName) => Some(name.decodedName.toString)
      case Select(This(_), name: TermName) => Some(name.decodedName.toString)
      case _ => None
    }

    // Match the %(x) explicit variable name specifications in parts and get final variable names
    val filtered: Seq[(String /* part */, String /* arg name */, c.Tree /* value */)] =
      for {
        idx <- args.indices
        argName = inferredNames(idx)
        part = parts(idx + 1)
      } yield {
        if (part.startsWith(StringUtils.VariableCharacter.toString + StringUtils.VariableParentheses._1)) {
          val pos = part.indexOf(StringUtils.VariableParentheses._2)
          val name = part.substring(2, pos)
          val filtered = part.substring(pos + 1)

          (filtered, name, args(idx))
        } else {
          if (argName.isEmpty)
            c.abort(c.enclosingPosition, s"No name is defined for part #$idx (${Compat.prettyPrint(c)(args(idx))})")

          (part, argName.get, args(idx))
        }
      }

    (parts.head + filtered.map {
      case (part, name, _) => s"%($name)$part"
    }.mkString, filtered.map {
      case (_, name, value) => q"($name, $value)"
    })
  }

  /**
    * Matches string against string literal pattern and return literal string if matched. Currently supported
    * literal types:
    *
    * 1) Plain literals like `"123"`
    * 2) Strip margin literals like `""" ... """.stripMargin`
    * 3) Strip margin literals with custom margin character like `""" ... """.stripMargin('#')`
    *
    * @param c Macro context
    * @param e Tree to match
    * @return Extracted string literal
    */
  private def stringLiteral(c: Context)(e: c.Tree): String = {
    import c.universe._

    def stripMargin(str: Tree, ch: Tree): String = (str, ch) match {
      case (Literal(Constant(s: String)), Literal(Constant(c: Char))) => s.stripMargin(c).trim
      case (Literal(Constant(s: String)), EmptyTree) => s.stripMargin.trim
      case (Literal(Constant(_: String)), _) =>
        c.abort(c.enclosingPosition, s"Expected character literal, got instead ${prettyPrint(c)(ch)}")
      case _ => c.abort(c.enclosingPosition, s"Expected string literal, got instead ${prettyPrint(c)(str)}")
    }

    e match {
      case Literal(Constant(s: String)) => s

      case q"scala.this.Predef.augmentString($str).stripMargin" => stripMargin(str, EmptyTree) // 2.11
      case q"scala.Predef.augmentString($str).stripMargin" => stripMargin(str, EmptyTree) // 2.12

      case q"scala.this.Predef.augmentString($str).stripMargin($ch)" => stripMargin(str, ch) // 2.11
      case q"scala.Predef.augmentString($str).stripMargin($ch)" => stripMargin(str, ch) // 2.12

      case _ =>
        c.abort(c.enclosingPosition, s"Expected string literal or multi-line string, got instead ${prettyPrint(c)(e)}")
    }
  }

  /**
    * Matches string against tuple `(String, T)` pattern and returns extracted string literal and tuple value.
    * Currently supported literal types:
    *
    * 1) Plain literals like `("1", x)`
    * 2) ArrowAssoc literals like `"1" -> x`
    *
    * @param c Macro context
    * @param e Tree to match
    * @return Extracted tuple literal parts
    */
  private def tupleLiteral(c: Context)(e: c.Tree): (String, c.Tree) = {
    import c.universe._

    val (a, b) = e match {
      case q"scala.Predef.ArrowAssoc[$aType]($ax).->[$bType]($bx)" => (ax, bx) // 2.12
      case q"scala.this.Predef.ArrowAssoc[$aType]($ax).->[$bType]($bx)" => (ax, bx) // 2.11
      case q"scala.this.Predef.any2ArrowAssoc[$aType]($ax).->[$bType]($bx)" => (ax, bx) // 2.10
      case q"($ax, $bx)" => (ax, bx)
      case _ =>
        c.abort(c.enclosingPosition, s"Expected tuple definition `x -> y` or `(x, y)`, got instead ${prettyPrint(c)(e)}")
    }

    (stringLiteral(c)(a), b)
  }
}
