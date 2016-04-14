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

import scala.language.implicitConversions

object LValue {
  implicit def unwrapLvalue[T](lValue: LValue[T])(implicit lang: Language): T = lValue.resolve
}

/**
  * Value that can be translated lazily (e.g. for definitions whose target language is not known a priori)
  */
class LValue[T](func: Language => T) {
  def resolve(implicit lang: Language) = func(lang)
  override def toString: String = s"LValue(${func(Language.English)})"
}
