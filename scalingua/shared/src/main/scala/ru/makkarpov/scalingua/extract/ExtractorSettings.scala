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

import java.io.File

import ru.makkarpov.scalingua.Compat._

object ExtractorSettings {
  val SettingsPrefix = "scalingua:"

  def fromContext(c: Context): ExtractorSettings = {
    val setts = c.settings
      .filter(_.startsWith(SettingsPrefix))
      .map(_.substring(SettingsPrefix.length).split("=", 2))
      .map{
        case Array(k, v) => k -> v
        case Array(x) => c.abort(c.enclosingPosition, s"Invalid setting: `$x`")
      }.toMap

    val enable = setts.contains("target")
    val targetFile = new File(setts.getOrElse("target", "messages.pot"))
    val baseDir = setts.getOrElse("baseDir", "")
    val taggedFile = setts.get("taggedFile").map(new File(_)).filter(_.exists)
    val implicitContext = setts.get("implicitContext").filter(_.nonEmpty)
    val escapeUnicode = setts.get("escapeUnicode").exists(_ == "true")

    ExtractorSettings(enable, new File(baseDir), targetFile, implicitContext, taggedFile, escapeUnicode)
  }
}

case class ExtractorSettings(enable: Boolean, srcBaseDir: File, targetFile: File, implicitContext: Option[String],
                             taggedFile: Option[File], escapeUnicode: Boolean) {
  def mergeContext(ctx: Option[String]): Option[String] = (implicitContext, ctx) match {
    case (None,    None)    => None
    case (Some(x), None)    => Some(x)
    case (None,    Some(y)) => Some(y)
    case (Some(x), Some(y)) => Some(x + ":" + y)
  }
}
