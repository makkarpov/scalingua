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

/**
  * Base trait for objects reprensenting language files. Can be implicitly summoned from `Messages` and `LanguageId`.
  */
object Language {
  implicit def $providedLanguage(implicit msg: Messages, lang: LanguageId): Language = msg.apply(lang)

  val English: Language = new Language {
    override def id = LanguageId("en", "US")

    override def singular(msgid: String): String = msgid
    override def singular(msgctx: String, msgid: String): String = msgid
    override def plural(msgid: String, msgidPlural: String, n: Long): String = if (n != 1) msgidPlural else msgid
    override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
      if (n != 1) msgidPlural else msgid
  }
}

trait Language {
  def id: LanguageId
  def singular(msgid: String): String
  def singular(msgctx: String, msgid: String): String
  def plural(msgid: String, msgidPlural: String, n: Long): String
  def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String
}
