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

import java.io.{BufferedReader, DataInputStream, FileInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import ru.makkarpov.scalingua.LanguageId
import sbt._

object GenerationContext {
  val HashMarker = "## Hash: ## "
  val ScalaHashPrefix = s"// $HashMarker"
}

case class GenerationContext(pkg: String, implicitCtx: Option[String], lang: LanguageId, hasTags: Boolean,
                             src: File, target: File, log: Logger)
{
  val srcHash = src.hashString

  def mergeContext(ctx: Option[String]): Option[String] = (implicitCtx, ctx) match {
    case (None,    None)    => None
    case (Some(x), None)    => Some(x)
    case (None,    Some(y)) => Some(y)
    case (Some(x), Some(y)) => Some(x + ":" + y)
  }

  def filePrefix = "/" + pkg.replace('.', '/') + (if (pkg.nonEmpty) "/" else "")

  def checkBinaryHash: Boolean = target.exists() && {
    val storedHash = {
      val is = new DataInputStream(new FileInputStream(target))
      try is.readUTF()
      catch {
        case t: Throwable =>
          t.printStackTrace()
          ""
      } finally is.close()
    }

    srcHash == storedHash
  }

  def checkTextHash: Boolean = target.exists() && {
    import GenerationContext.HashMarker

    val storedHash = {
      val rd = new BufferedReader(new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8))
      try {
        val l = rd.readLine()
        if ((l ne null) && l.contains(HashMarker)) {
          val idx = l.indexOf(HashMarker)
          l.substring(idx + HashMarker.length)
        } else ""
      } catch {
        case t: Throwable =>
          t.printStackTrace()
          ""
      } finally rd.close()
    }

    srcHash == storedHash
  }
}