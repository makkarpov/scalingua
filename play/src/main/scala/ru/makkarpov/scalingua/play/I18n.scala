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

package ru.makkarpov.scalingua.play

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import ru.makkarpov.scalingua
import ru.makkarpov.scalingua._

import scala.language.experimental.macros
import scala.language.implicitConversions

trait I18n extends scalingua.twirl.I18n {
  // The conversion from `RequestHeader` to `LanguageId` will imply information loss:
  // Suppose browser sent the following language precedence list `xx_YY`, `zz_WW`, `en_US`.
  // This conversion will have no information about available languages, so it will result in
  // `LanguageId("xx", "YY")` even if this language is absent. By supplying implicit `Messages`
  // too, we will have enough information to skip unsupported languages and return supported
  // one with respect to priority.

  implicit def requestHeader2Language(implicit rq: RequestHeader, msg: Messages): Language =
    rq.headers.get(HeaderNames.ACCEPT_LANGUAGE).map(PlayUtils.languageFromAccept).getOrElse(Language.English)
}

object I18n extends I18n