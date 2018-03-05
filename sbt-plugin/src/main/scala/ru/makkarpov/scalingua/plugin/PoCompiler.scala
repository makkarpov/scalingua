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

package ru.makkarpov.scalingua.plugin

import java.io._
import java.nio.charset.StandardCharsets
import java_cup.runtime.ComplexSymbolFactory.Location

import ru.makkarpov.scalingua.LanguageId
import ru.makkarpov.scalingua.extract.TaggedParser
import ru.makkarpov.scalingua.plural.ParsedPlural
import ru.makkarpov.scalingua.pofile.parse.{LexerException, ParserException}
import ru.makkarpov.scalingua.pofile.{Message, MultipartString, PoFile}

object PoCompiler {
  val EndOfFile     = 0
  val Singular      = 1
  val SingularCtx   = 2
  val Plural        = 3
  val PluralCtx     = 4
  val SingularTag   = 5
  val PluralTag     = 6

  val EnglishTagsClass = "CompiledEnglishTags"

  def catchErrors[R](ctx: GenerationContext)(f: => R): R = {
    def report(left: Location, right: Location, msg: String, t: Throwable): Nothing = {
      val lineNum = left.getLine
      val ofsStart = left.getColumn

      val rd = new BufferedReader(new InputStreamReader(new FileInputStream(ctx.src), StandardCharsets.UTF_8))
      var line: String = ""

      for (_ <- 0 to lineNum) line = rd.readLine()

      if (line == null) ctx.log.error(s"at ${ctx.src.getName}:$lineNum:$ofsStart: $msg")
      else {
        val ofsEnd =
          if (right.getLine > left.getLine) line.length
          else if (right.getColumn > line.length) line.length
          else if (right.getColumn <= left.getColumn) left.getColumn + 1
          else right.getColumn

        val subscript = " " * ofsStart + (if (ofsEnd - ofsStart <= 1) "^" else "~") * (ofsEnd - ofsStart)

        ctx.log.error(s"at ${ctx.src.getName}:$lineNum:$ofsStart: $msg")
        ctx.log.error(s"")
        ctx.log.error(s"$line")
        ctx.log.error(s"$subscript")
      }

      throw ParseFailedException(s"failed to parse ${ctx.src.getCanonicalPath}", t)
    }

    try f
    catch {
      case p: ParserException => report(p.left, p.right, p.msg, p)
      case l: LexerException => report(l.loc, l.loc, l.msg, l)
    }
  }

  def doPackaging(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.checkBinaryHash)
      return

    val dos = new DataOutputStream(new FileOutputStream(ctx.target))
    try {
      dos.writeUTF(ctx.srcHash)

      dos.writeUTF(ctx.lang.language)
      dos.writeUTF(ctx.lang.country)

      def writePlurals(trs: Seq[MultipartString]): Unit = {
        dos.writeByte(trs.size)
        for (s <- trs) dos.writeUTF(s.merge)
      }

      catchErrors(ctx) {
        for (m <- PoFile(ctx.src)) m match {
          case Message.Singular(hdr, ctxt, id, tr) =>
            hdr.tag match {
              case Some(tag) =>
                dos.writeByte(SingularTag)
                dos.writeUTF(tag)

              case None =>
                ctx.mergeContext(ctxt.map(_.merge)) match {
                  case None => dos.writeByte(Singular)
                  case Some(x) =>
                    dos.writeByte(SingularCtx)
                    dos.writeUTF(x)
                }

                dos.writeUTF(id.merge)
            }

            dos.writeUTF(tr.merge)

          case Message.Plural(hdr, ctxt, id, _, trs) =>
            hdr.tag match {
              case Some(tag) =>
                dos.writeByte(PluralTag)
                dos.writeUTF(tag)

              case None =>
                ctx.mergeContext(ctxt.map(_.merge)) match {
                  case None => dos.writeByte(3)
                  case Some(x) =>
                    dos.writeByte(4)
                    dos.writeUTF(x)
                }

                dos.writeUTF(id.merge)
            }

            writePlurals(trs)
        }
      }

      dos.writeByte(EndOfFile)
    } finally dos.close()
  }

  def doCompiling(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.checkTextHash)
      return

    val hdr = catchErrors(ctx) {
      val iter = PoFile(ctx.src)
      try iter.find(_.message.isEmpty)
      finally iter.size // Consume whole iterator and close stream
    }.map {
      case Message.Singular(_, _, _, tr) => tr.merge
      case Message.Plural(_, _, _, _, trs) => trs.headOption.map(_.merge).getOrElse("")
    }.map(_.trim.split("\n").map(_.split(":", 2))).map(_.map {
      case Array(k, v) => k.trim -> v.trim
      case Array(x) => throw new IllegalArgumentException(s"Invalid header string: '$x'")
    }.toMap).getOrElse(Map.empty)

    val pf = hdr.get("Plural-Forms") match {
      case None =>
        ctx.log.warn(s"No `Plural-Forms` header in language `${ctx.lang}`, assuming English.")
        ParsedPlural.English

      case Some(x) => ParsedPlural.fromHeader(x)
    }

    val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ctx.target), StandardCharsets.UTF_8), false)
    try {
      pw.print(
        s"""${GenerationContext.ScalaHashPrefix}${ctx.srcHash}
           |${if (ctx.pkg.nonEmpty) s"package ${ctx.pkg}" else ""}
           |
           |import ru.makkarpov.scalingua.{CompiledLanguage, PluralFunction, TaggedLanguage}
           |
           |object Language_${ctx.lang.language}_${ctx.lang.country}
           |extends CompiledLanguage with PluralFunction {
           |  initialize({
           |    val str = getClass.getResourceAsStream("${ctx.filePrefix}data_${ctx.lang.language}_${ctx.lang.country}.bin")
           |    if (str eq null) {
           |      throw new IllegalArgumentException("Resource not found for language ${ctx.lang.language}_${ctx.lang.country}")
           |    }
           |    str
           |  })
           |
           |  val numPlurals = ${pf.numPlurals}
           |  def plural(arg: Long): Int = (${pf.expr.scalaExpression}).toInt
           |  def taggedFallback: TaggedLanguage = ${if (ctx.hasTags) EnglishTagsClass else "TaggedLanguage.Identity"}
           |}
         """.stripMargin)
    } finally pw.close()
  }

  def generateIndex(pkg: String, tgt: File, langs: Seq[LanguageId], hasTags: Boolean): Unit = {
    val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tgt), StandardCharsets.UTF_8), false)
    try {
      pw.print(
        s"""${if (pkg.nonEmpty) s"package $pkg" else ""}
           |
           |import ru.makkarpov.scalingua.{Messages, TaggedLanguage}
           |
           |object Languages extends Messages(${if (hasTags) EnglishTagsClass else "TaggedLanguage.Identity"}${if (langs.nonEmpty) "," else ""}
           |  ${langs.map(l => s"Language_${l.language}_${l.country}").mkString(",\n  ")}
           |)
         """.stripMargin
      )
    } finally pw.close()
  }

  def packageEnglishTags(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.checkBinaryHash)
      return

    val dos = new DataOutputStream(new FileOutputStream(ctx.target))
    try {
      dos.writeUTF(ctx.srcHash)

      for (m <- TaggedParser.parse(ctx.src))
        m.plural match {
          case None =>
            dos.writeByte(SingularTag)
            dos.writeUTF(m.tag)
            dos.writeUTF(m.msg)

          case Some(plural) =>
            dos.writeByte(PluralTag)
            dos.writeUTF(m.tag)
            dos.writeUTF(m.msg)
            dos.writeUTF(plural)
        }

      dos.writeByte(EndOfFile)
    } finally dos.close()
  }

  def compileEnglishTags(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.checkTextHash)
      return

    val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ctx.target), StandardCharsets.UTF_8), false)
    try {
      pw.print(
        s"""${GenerationContext.ScalaHashPrefix}${ctx.srcHash}
           |${if (ctx.pkg.nonEmpty) s"package ${ctx.pkg}" else ""}
           |
           |import ru.makkarpov.scalingua.CompiledLanguage.EnglishTags
           |
           |object $EnglishTagsClass extends EnglishTags {
           |  initialize({
           |    val str = getClass.getResourceAsStream("${ctx.filePrefix}compiled_english_tags.bin")
           |    if (str eq null)
           |      throw new NullPointerException("Compiled english tags not found!")
           |    str
           |  })
           |}
         """.stripMargin
      )
    } finally pw.close()
  }
}
