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

package ru.makkarpov.scalingua.test

import ru.makkarpov.scalingua.{Language, LanguageId}

class MockLang(s: String) extends Language {
  override def id: LanguageId = LanguageId("mock", "p" + s)
  override def singular(msgid: String): String = s"{s$s:$msgid}"
  override def singular(msgctx: String, msgid: String): String = s"{sc$s:$msgctx:$msgid}"
  override def plural(msgid: String, msgidPlural: String, n: Long): String = s"{p$s:$msgid:$msgidPlural:$n}"
  override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
    s"{pc$s:$msgctx:$msgid:$msgidPlural:$n}"
}