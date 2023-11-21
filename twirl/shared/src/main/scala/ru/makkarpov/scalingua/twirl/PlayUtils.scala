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

package ru.makkarpov.scalingua.twirl

import play.twirl.api.Html
import ru.makkarpov.scalingua._

import scala.language.experimental.macros

object PlayUtils {
  class StringInterpolator(val sc: StringContext) extends AnyVal {
    def h(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
      macro Macros.interpolate[Html]

    def lh(args: Any*)(implicit outputFormat: OutputFormat[Html]): LValue[Html] =
      macro Macros.lazyInterpolate[Html]

    def ph(args: Any*)(implicit lang: Language, outputFormat: OutputFormat[Html]): Html =
      macro Macros.pluralInterpolate[Html]

    def lph(args: Any*)(outputFormat: OutputFormat[Html]): LValue[Html] =
      macro Macros.lazyPluralInterpolate[Html]
  }
}
