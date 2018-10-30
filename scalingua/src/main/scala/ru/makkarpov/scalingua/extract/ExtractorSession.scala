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

package ru.makkarpov.scalingua.extract

import java.io.{File, IOException}

import ru.makkarpov.scalingua.Compat.Context
import ru.makkarpov.scalingua.extract.ExtractorSession.MutableMessage
import ru.makkarpov.scalingua.extract.MessageExtractor.setupSession
import ru.makkarpov.scalingua.pofile._

import scala.collection.mutable
import scala.reflect.api.Position
import scala.reflect.internal.{NoPhase, Phase}
import scala.reflect.macros.Universe

object ExtractorSession {
  class MutableMessage {
    val comments = mutable.ListBuffer.empty[String]
    val extractedComments = mutable.ListBuffer.empty[String]
    val locations = mutable.Set.empty[MessageLocation]
    var flags = MessageFlag.ValueSet.empty

    var context = Option.empty[MultipartString]
    var msgid = MultipartString.empty
    var msgidPlural = Option.empty[MultipartString]

    var translations = Seq.empty[MultipartString]
    var tag = Option.empty[String]

    def :=(msg: Message): Unit = {
      comments.clear()
      comments ++= msg.header.comments
      extractedComments.clear()
      extractedComments ++= msg.header.extractedComments
      locations.clear()
      locations ++= msg.header.locations
      tag = msg.header.tag.orElse(tag)
      flags = msg.header.flags
      context = msg.context
      msgid = msg.message

      msg match {
        case Message.Singular(_, _, _, tr) =>
          msgidPlural = None
          translations = tr :: Nil
        case Message.Plural(_, _, _, pl, tr) =>
          msgidPlural = Some(pl)
          translations = tr
      }
    }

    def toMsg: Message = {
      val header = MessageHeader(comments, extractedComments, locations.toSeq, flags, tag)

      msgidPlural match {
        case None => Message.Singular(header, context, msgid, translations.headOption.getOrElse(MultipartString.empty))
        case Some(pl) => Message.Plural(header, context, msgid, pl, translations)
      }
    }
  }
}

class ExtractorSession(val global: Universe, val setts: ExtractorSettings) {
  private var _finished = false

  /* Since macros don't know when compiler will terminate, we will try to make compiler tell it to us.
   * Seems that the most easy way to do it - push a new phase and wait until it will be invoked.
   * It seems to be pretty scary since it is deep compiler internals, so please tell me if it will break
   * some day.
   */
  try {
    val symbolTable = Class.forName("scala.reflect.internal.SymbolTable")
    val currentPhase = symbolTable.getMethod("phase").invoke(global).asInstanceOf[Phase]

    def allPhases: Seq[Phase] = {
      val r = Seq.newBuilder[Phase]
      var c = currentPhase

      while (c.prev != NoPhase)
        c = c.prev

      var flag = true
      while (flag) {
        r += c
        if (c.hasNext) c = c.next
        else flag = false
      }

      r.result()
    }

    def printPhases: String = {
      val ret = new StringBuilder

      for ((x, i) <- allPhases.zipWithIndex)
        ret ++= f" $i%02d: ${x.name}%s (id = ${x.id}%02d, prev = '${x.prev.name}%s', next = '${x.next.name}%s', hasNext = ${x.hasNext}%s)%n"

      ret.result()
    }

    def insertBefore(next: Phase): Unit = {
      val prev = next.prev

      if (prev.name == "save-translations")
        throw new IOException("Attempting to append phase twice!")

      val own = new Phase(prev) {
        override def name: String = "save-translations"
        override def run(): Unit = ExtractorSession.this.finish()
      }

      // Now phases have correct pointers:
      //   own.prev = prev
      //   prev.next = own
      // To be adjusted:
      //   next.prev -> own
      //   own.next -> next

      val nx = classOf[Phase].getDeclaredMethods.find(_.getName.contains("nx_$eq")).getOrElse(sys.error("Cannot find `nx_$eq` method!"))
      nx.setAccessible(true)
      nx.invoke(own, next)

      // Seems to be unnecessary, because no-one refers `prev` pointer.
//      val pw = classOf[Phase].getDeclaredField("prev")
//      pw.setAccessible(true)
//
//      val mods = classOf[Field].getDeclaredField("modifiers")
//      mods.setAccessible(true)
//      mods.setInt(pw, pw.getModifiers & ~Modifier.FINAL)
//
//      pw.set(next, own)
    }

    insertBefore(allPhases.find(_.name == "terminal").getOrElse {
      throw new IllegalArgumentException(s"No terminal phase in compiler: \n$printPhases")
    })
  } catch {
    case t: Throwable =>
      Console.err.println(
        """+=====================================================================+
          || Cannot inject a next phase into compiler to save translations.      |
          || Translations will be saved using `Runtime.addShutdownHook`, but it  |
          || can cause issues in SBT where JVM does not fork when compiling.     |
          ||                                                                     |
          || Please report this issue to Github!                                 |
          |+=====================================================================+""".stripMargin)

      t.printStackTrace()

      Runtime.getRuntime.addShutdownHook(new Thread {
        setName(s"Translation save hook #${System.identityHashCode(ExtractorSession.this.global)}")

        override def run(): Unit = ExtractorSession.this.finish()
      })
  }

  private val byFile = mutable.Map.empty[File, List[MutableMessage]]
  private val byMsgid = mutable.Map.empty[(String, Option[String]), MutableMessage]

  if (setts.enable) {
    if (setts.targetFile.exists()) {
      for (m <- PoFile(setts.targetFile) if !m.header.isTagged) {
        val mm = new MutableMessage
        mm := m
        for (loc <- mm.locations) {
          val lst = byFile.getOrElse(loc.file, Nil)
          byFile(loc.file) = mm :: lst
        }

        byMsgid(mm.msgid.merge -> mm.context.map(_.merge)) = mm
      }
    }


    for (t <- setts.taggedFile; m <- TaggedParser.parse(t)) {
      message(m.msg, None) := m.toMessage
    }
  }

  private def location(pos: Position): MessageLocation = {
    val srcFile = pos.source
    val f = Option(srcFile.file.file).collect {
      case f if f.toPath.startsWith(setts.srcBaseDir.toPath) => setts.srcBaseDir.toPath.relativize(f.toPath).toFile
    }.getOrElse(new File(srcFile.path))
    MessageLocation(f, pos.line)
  }

  private def flushFile(pos: Position): Unit = {
    val f = location(pos).file

    byFile.get(f) match {
      case Some(xs) =>
        for (x <- xs) x.locations.retain(_.file != f)
        byFile.remove(f)

      case None => // all ok
    }
  }

  private def message(msgid: String, msgctxt: Option[String]): MutableMessage = {
    byMsgid.get(msgid -> msgctxt) match {
      case Some(m) => m
      case None =>
        val r = new MutableMessage
        r.msgid = MultipartString(msgid)
        r.context = msgctxt.map(MultipartString.apply)
        byMsgid.put(msgid -> msgctxt, r)
        r
    }
  }

  def finish(): Unit = {
    if (_finished) return
    _finished = true

    if (setts.enable) {
      val parent = setts.targetFile.getParentFile
      if ((parent ne null) && !parent.exists() && !parent.mkdirs())
        throw new IOException(s"Cannot create directory ${parent.getCanonicalPath}!")

      val msgs = byMsgid.valuesIterator.filter(_.locations.nonEmpty).toSeq.sortBy(_.locations.min).map(_.toMsg)
      PoFile.update(setts.targetFile, msgs, setts.escapeUnicode)
    }
  }

  def put(msgctx: Option[String], msgid: String, msgidPlural: Option[String], position: Position): Unit = {
    if (!setts.enable)
      return

    flushFile(position)

    val loc = location(position)

    val msg = message(msgid, msgctx)

    msg.locations += loc
    if (msg.msgidPlural.isEmpty)
      msg.msgidPlural = msgidPlural.map(MultipartString.apply)

    if (msg.msgidPlural.isDefined)
      msg.translations = Seq(MultipartString.empty, MultipartString.empty)
    else
      msg.translations = Seq(MultipartString.empty)
  }

  def singular(c: Context)(msgctx: Option[String], msgid: String): Unit =
    setupSession(c).put(msgctx, msgid, None, c.enclosingPosition)

  def plural(c: Context)(msgctx: Option[String], msgid: String, msgidPlural: String): Unit =
    setupSession(c).put(msgctx, msgid, Some(msgidPlural), c.enclosingPosition)
}
