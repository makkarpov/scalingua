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

package ru.makkarpov.scalingua.plural

/**
  * Represents plural suffixes that will be understood by macros. If tree typechecks to either `Suffix.S` or
  * `Suffix.ES`, it's considered as plural suffix. No instances of `Suffix.*` exists.
  */
object Suffix {
  sealed trait Generic extends Suffix
  sealed trait S extends Suffix
  sealed trait ES extends Suffix

  case class GenericSuffixExtension(s: String) extends AnyVal {
    def &>(plur: String): Suffix.Generic =
      throw new IllegalArgumentException("&> should not remain after macro expansion")
  }

  def s: S = throw new IllegalArgumentException(".s or .es should not remain after macro expansion")
  def es: ES = throw new IllegalArgumentException(".s or .es should not remain after macro expansion")
}

sealed trait Suffix