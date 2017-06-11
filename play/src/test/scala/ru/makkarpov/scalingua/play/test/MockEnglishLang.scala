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

package ru.makkarpov.scalingua.play.test

import ru.makkarpov.scalingua.{Language, LanguageId}

case class MockEnglishLang(lid: String) extends Language {
  override val id: LanguageId = LanguageId(lid)

  override def singular(msgid: String): String = msgid
  override def singular(msgctx: String, msgid: String): String = msgid
  override def plural(msgid: String, msgidPlural: String, n: Long): String = if (n == 1) msgid else msgidPlural
  override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
    if (n == 1) msgid else msgidPlural

  override def merge(other: Language): Language = throw new NotImplementedError("MockEnglishLang.merge")
}
