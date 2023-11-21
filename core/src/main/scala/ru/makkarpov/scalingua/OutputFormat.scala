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

object OutputFormat {
  /**
    * String interpolation format that does nothing at all it's already strings.
    */
  implicit val StringFormat: OutputFormat[String] = new OutputFormat[String] {
    override def convert(s: String): String = s
    override def escape(s: String): String = s
  }
}

/**
  * An implicit evidence that strings could be interpolated into type `R`.
  *
  * @tparam R Result type of interpolation
  */
trait OutputFormat[R] {
  /**
    * Convert resulting string into type `R`
    *
    * @param s Complete interpolated string
    * @return An instance of `R`
    */
  def convert(s: String): R

  /**
    * Escape interpolation variable.
    *
    * @param s A string contents of interpolation variable
    * @return A escaped string that will be inserted into interpolation output
    */
  def escape(s: String): String
}
