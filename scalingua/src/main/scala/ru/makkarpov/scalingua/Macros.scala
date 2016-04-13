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
import MacroUtils._

/**
  * An entry point for all macros which may be useful if you want to define custom functions (like your own `Utils.t`
  * referencing this macros) or custom specialized string interpolators (like `th""` for HTML). Without this class it
  * would be impossible, since macros will expand at your `Utils` class and complain that string literal is required
  * for I18n message.
  */
object Macros {
  /**
    * String interpolator that will infer the name of the variables passed in it and create an interpolation string
    * based on them. For example, `t"Hello, \$name"` will be converted to string `"Hello, %(name)!"`. If interpolation
    * variable is a complex expression, you can pass the name after it, like `t"2 + 2 is \${2 + 2}%(result)"`, so the
    * interpolator will use `"result"` as name of expression `2 + 2`.
    */
  def interpolate[T: c.WeakTypeTag]
    (c: Context)
    (args: c.Expr[Any]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {
    import c.universe._

    val parts = c.prefix.tree match {
      case Apply(_, List(Apply(_, rawParts))) =>
        rawParts.map(stringLiteral(c)(_)).map(StringContext.treatEscapes)
      case _ =>
        c.abort(c.enclosingPosition, "Failed to detect application context")
    }

    assert(parts.size == args.size + 1)

    val inferredNames = args.map(_.tree).map {
      case Ident(name: TermName) => Some(name.decodedName.toString)
      case Select(This(_), name: TermName) => Some(name.decodedName.toString)
      case _ => None
    }

    val filtered: Seq[(String /* part */, String /* arg name */, c.Expr[Any] /* value */)] =
      for {
        idx <- 0 until args.size
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
            c.abort(c.enclosingPosition, s"No name is defined for part #$idx (${Compat.prettyPrint(c)(args(idx).tree)})")

          (part, argName.get, args(idx))
        }
      }

    for ((name, vals) <- filtered.groupBy(_._2) if vals.exists(_ != vals.head))
      c.abort(c.enclosingPosition, s"Duplicate variable name: $name")

    val msgid = parts.head + filtered.map {
      case (part, name, _) => s"%($name)$part"
    }.mkString

    val tr = filtered.groupBy(_._2).mapValues(_.head._3.tree)

    generateSingular[T](c)(None, msgid, tr)(lang, outputFormat).asInstanceOf[c.Expr[T]]
  }

  /**
    * The whole purpose of this macro, beside of extraction of strings, is to verify that all string interpolation
    * variables are present and to omit insertion of `StringUtils.interpolate` if nothing is dynamic.
    */
  def singular[T: c.WeakTypeTag]
    (c: Context)
    (msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {

    val (msgid, vars) = verifyVariables(c)(msg, args, None)

    generateSingular[T](c)(None, msgid, vars)(lang, outputFormat).asInstanceOf[c.Expr[T]]
  }

  /**
    * The whole purpose of this macro, beside of extraction of strings, is to verify that all string interpolation
    * variables are present and to omit insertion of `StringUtils.interpolate` if nothing is dynamic.
    */
  def singularCtx[T: c.WeakTypeTag]
    (c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {

    val ctxStr = stringLiteral(c)(ctx.tree)
    val (msgid, vars) = verifyVariables(c)(msg, args, None)

    generateSingular[T](c)(Some(ctxStr), msgid, vars)(lang, outputFormat).asInstanceOf[c.Expr[T]]
  }

  /**
    * The whole purpose of this macro, beside of extraction of strings, is to verify that all string interpolation
    * variables are present and to omit insertion of `StringUtils.interpolate` if nothing is dynamic.
    */
  def plural[T: c.WeakTypeTag]
    (c: Context)
    (msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {

    val (msgid, vars) = verifyVariables(c)(msg, args, Some(n))
    val (msgidPlural, _) = verifyVariables(c)(msgPlural, args, Some(n))

    generatePlural[T](c)(None, msgid, msgidPlural, n, vars)(lang, outputFormat).asInstanceOf[c.Expr[T]]
  }

  /**
    * The whole purpose of this macro, beside of extraction of strings, is to verify that all string interpolation
    * variables are present and to omit insertion of `StringUtils.interpolate` if nothing is dynamic.
    */
  def pluralCtx[T: c.WeakTypeTag]
    (c: Context)
    (ctx: c.Expr[String], msg: c.Expr[String], msgPlural: c.Expr[String], n: c.Expr[Long], args: c.Expr[(String, Any)]*)
    (lang: c.Expr[Language], outputFormat: c.Expr[OutputFormat[T]]): c.Expr[T] =
  {

    val ctxStr = stringLiteral(c)(ctx.tree)
    val (msgid, vars) = verifyVariables(c)(msg, args, Some(n))
    val (msgidPlural, _) = verifyVariables(c)(msgPlural, args, Some(n))

    generatePlural[T](c)(Some(ctxStr), msgid, msgidPlural, n, vars)(lang, outputFormat).asInstanceOf[c.Expr[T]]
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def verifyVariables
    (c: Context)
    (msg: c.Expr[String], args: Seq[c.Expr[(String, Any)]], n: Option[c.Expr[Long]]): (String, Map[String, c.Tree]) =
  {
    val msgStr = stringLiteral(c)(msg.tree)
    val vars = StringUtils.extractVariables(msgStr)

    var exprs = args.map(tupleLiteral(c)(_))

    for (nx <- n)
      exprs :+= "n" -> nx

    // Test uniqueness of variables:
    for ((v, _) <- exprs.groupBy(_._1).filter(_._2.size > 1))
      c.abort(c.enclosingPosition, s"Duplicate variable `$v`")

    // Test difference of variables

    val argVars = exprs.map(_._1).toSet

    for (n <- (vars diff argVars) ++ (argVars diff vars))
      if (vars.contains(n))
        c.abort(c.enclosingPosition, s"Variable `$n` is not present at argument list")
      else
        c.abort(c.enclosingPosition, s"Variable `$n` is not present at interpolation string")

    (msgStr, Map(exprs:_*).mapValues(_.tree).asInstanceOf[Map[String, c.Tree]])
  }
}
