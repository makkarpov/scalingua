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

import ru.makkarpov.scalingua.LanguageId
import ru.makkarpov.scalingua.plural.ParsedPlural
import ru.makkarpov.scalingua.pofile.{Message, MultipartString, PoFile}

object PoCompiler {
  val ScalaHashPrefix = "//# Hash: "

  def doPackaging(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.target.exists()) {
      val storedHash = {
        val is = new DataInputStream(new FileInputStream(ctx.target))
        try is.readUTF()
        catch {
          case t: Throwable =>
            t.printStackTrace()
            ""
        } finally is.close()
      }

      if (ctx.srcHash == storedHash)
        return
    }

    val dos = new DataOutputStream(new FileOutputStream(ctx.target))
    try {
      dos.writeUTF(ctx.srcHash)

      dos.writeUTF(ctx.lang.language)
      dos.writeUTF(ctx.lang.country)

      def writePlurals(trs: Seq[MultipartString]): Unit = {
        dos.writeByte(trs.size)
        for (s <- trs) dos.writeUTF(s.merge)
      }

      for (m <- PoFile(ctx.src)) m match {
        case Message.Singular(_, ctxt, id, tr) =>
          ctx.mergeContext(ctxt.map(_.merge)) match {
            case None => dos.writeByte(1)
            case Some(x) =>
              dos.writeByte(2)
              dos.writeUTF(x)
          }

          dos.writeUTF(id.merge)
          dos.writeUTF(tr.merge)

        case Message.Plural(_, ctxt, id, _, trs) =>
          ctx.mergeContext(ctxt.map(_.merge)) match {
            case None => dos.writeByte(3)
            case Some(x) =>
              dos.writeByte(4)
              dos.writeUTF(x)
          }

          dos.writeUTF(id.merge)
          writePlurals(trs)
      }

      dos.writeByte(0)
    } finally dos.close()
  }

  def doCompiling(ctx: GenerationContext): Unit = {
    // Should we regenerate file?
    if (ctx.target.exists()) {
      val storedHash = {
        val rd = new BufferedReader(new InputStreamReader(new FileInputStream(ctx.target), StandardCharsets.UTF_8))
        try {
          val l = rd.readLine()
          if ((l ne null) && l.startsWith(ScalaHashPrefix)) l.substring(ScalaHashPrefix.length).trim
          else ""
        } catch {
          case t: Throwable =>
            t.printStackTrace()
            ""
        } finally rd.close()
      }

      if (storedHash == ctx.srcHash)
        return
    }

    val hdr = {
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
        s"""$ScalaHashPrefix${ctx.srcHash}
           |${if (ctx.pkg.nonEmpty) s"package ${ctx.pkg}" else ""}
           |
           |import ru.makkarpov.scalingua.{CompiledLanguage, PluralFunction}
           |
           |object Language_${ctx.lang.language}_${ctx.lang.country}
           |extends CompiledLanguage with PluralFunction {
           |  load()
           |
           |  def load(): Unit = {
           |    initialize({
           |      val str = getClass.getResourceAsStream("${ctx.filePrefix}data_${ctx.lang.language}_${ctx.lang.country}.bin")
           |      if (str eq null) {
           |        throw new IllegalArgumentException("Resource not found for language ${ctx.lang.language}_${ctx.lang.country}")
           |      }
           |      str
           |    })
           |  }
           |
           |  // Cached language data should be invalidated on JRebel reload
           |  def __rebelReload(): Unit = {
           |    load()
           |  }
           |
           |  val numPlurals = ${pf.numPlurals}
           |  def plural(arg: Long): Int = (${pf.expr.scalaExpression}).toInt
           |}
         """.stripMargin)
    } finally pw.close()
  }

  def generateIndex(pkg: String, tgt: File, langs: Seq[LanguageId]): Unit = {
    val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tgt), StandardCharsets.UTF_8), false)
    try {
      pw.print(
        s"""${if (pkg.nonEmpty) s"package $pkg" else ""}
           |
           |import ru.makkarpov.scalingua.Messages
           |
           |object Languages extends Messages(
           |  ${langs.map(l => s"Language_${l.language}_${l.country}").mkString(",\n  ")}
           |)
         """.stripMargin
      )
    } finally pw.close()
  }
}
