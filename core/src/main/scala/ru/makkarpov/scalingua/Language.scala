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

object Language {
  /**
    * Implicit conversion to derive language from available `LanguageId` and `Messages`
    */
  @inline
  implicit def $providedLanguage(implicit msg: Messages, lang: LanguageId): Language = msg.apply(lang)

  /**
    * A fallback English language that always returns the same message strings.
    */
  val English: Language = new Language {
    override def id = LanguageId("en", "US")

    override def singular(msgid: String): String = msgid
    override def singular(msgctx: String, msgid: String): String = msgid
    override def plural(msgid: String, msgidPlural: String, n: Long): String = if (n != 1) msgidPlural else msgid
    override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
      if (n != 1) msgidPlural else msgid
  }
}

/**
  * Base trait for objects reprensenting languages.
  */
trait Language {
  /**
    * A exact (with country part) ID of this language.
    */
  def id: LanguageId

  /**
    * Resolve singular form of message without a context.
    *
    * @param msgid A message to resolve
    * @return Resolved message or `msgid` itself.
    */
  def singular(msgid: String): String

  /**
    * Resolve singular form of message with a context.
    *
    * @param msgctx A context of message
    * @param msgid A message to resolve
    * @return Resolved message or `msgid` itself.
    */
  def singular(msgctx: String, msgid: String): String

  /**
    * Resolve plural form of message without a context
    *
    * @param msgid A singular form of message
    * @param msgidPlural A plural form of message
    * @param n Numeral representing which form to choose
    * @return Resolved plural form of message
    */
  def plural(msgid: String, msgidPlural: String, n: Long): String

  /**
    * Resolve plural form of message with a context.
    *
    * @param msgctx A context of message
    * @param msgid A singular form of message
    * @param msgidPlural A plural form of message
    * @param n Numeral representing which form to choose
    * @return Resolved plural form of message
    */
  def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String
}
