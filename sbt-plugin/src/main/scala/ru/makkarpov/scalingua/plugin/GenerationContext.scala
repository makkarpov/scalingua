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

import ru.makkarpov.scalingua.LanguageId
import sbt._

case class GenerationContext(pkg: String, implicitCtx: Option[String], lang: LanguageId, src: File, target: File,
                             log: Logger)
{
  val srcHash = src.hashString

  def mergeContext(ctx: Option[String]): Option[String] = (implicitCtx, ctx) match {
    case (None,    None)    => None
    case (Some(x), None)    => Some(x)
    case (None,    Some(y)) => Some(y)
    case (Some(x), Some(y)) => Some(x + ":" + y)
  }

  def filePrefix = "/" + pkg.replace('.', '/') + (if (pkg.nonEmpty) "/" else "")
}