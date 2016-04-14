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

import ru.makkarpov.scalingua.extract.MessageExtractor

import scala.reflect.macros.whitebox

object Compat {
  type Context = whitebox.Context
  def prettyPrint(c: Context)(e: c.Tree): String = c.universe.showCode(e)
  def termName(c: Context)(s: String): c.TermName = c.universe.TermName(c.freshName(s))
}
