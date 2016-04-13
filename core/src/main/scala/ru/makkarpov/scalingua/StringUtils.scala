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
  * Provides various string functions that are useful both in macros and in I18n code, such as string interpolations,
  * escaping and unescaping.
  */
object StringUtils {
  /**
    * Interpolation placeholder character
    */
  val VariableCharacter   = '%'

  /**
    * Opening and closing interpolation parentheses
    */
  val VariableParentheses = '(' -> ')'

  class InvalidInterpolationException(msg: String) extends IllegalArgumentException(msg)

  /**
    * Convert escape sequences like `\\n` to their meanings. Differs from `StringContext.treatEscapes` because latter
    * does not handle `\\uXXXX` escape codes.
    *
    * @param s String with literal escape codes
    * @return `s` having escape codes replaced with their meanings.
    */
  def unescape(s: String): String = {
    val ret = new StringBuilder
    ret.sizeHint(s.length)

    var cursor = 0
    while (cursor < s.length) {
      val pos = s.indexOf('\\', cursor)

      if (pos == -1) {
        ret ++= s.substring(cursor)
        cursor = Int.MaxValue
      } else {
        ret ++= s.substring(cursor, pos)

        if (pos + 1 >= s.length)
          throw new IllegalArgumentException(s"Unexpected end of stirng at index $pos")

        val escapeLength = s(pos + 1) match {
          case 'u' => 4
          case 'n' | 'r' | 't' | 'b' | 'f' | '\\' | '\'' | '\"' => 0
          case x => throw new IllegalArgumentException(s"Invalid escape character '\\$x' at index $pos")
        }

        if (pos + 1 + escapeLength >= s.length)
          throw new IllegalArgumentException(s"Unexpected end of string at index $pos")

        s(pos + 1) match {
          case 'n'  => ret += '\n'
          case 'r'  => ret += '\r'
          case 't'  => ret += '\t'
          case 'b'  => ret += '\b'
          case 'f'  => ret += '\f'
          case '\\' => ret += '\\'
          case '\'' => ret += '\''
          case '\"' => ret += '\"'
          case 'u'  => ret += Integer.parseInt(s.substring(pos + 2, pos + 6), 16).toChar
        }

        cursor = pos + 2 + escapeLength
      }
    }

    ret.result()
  }

  /**
    * Converts all non-letter and non-printable characters in `s` to their escape codes.
    *
    * @param s Raw string to escape
    * @return Escaped version of `s`
    */
  def escape(s: String): String = {
    val ret = new StringBuilder
    ret.sizeHint(s.length)

    def canPrintLiterally(c: Char) = Character.isLetter(c) || Character.isDigit(c) || ((c >= 32) && (c < 127))

    for (c <- s) c match {
      case '\n' => ret ++= "\\n"
      case '\r' => ret ++= "\\r"
      case '\t' => ret ++= "\\t"
      case '\b' => ret ++= "\\b"
      case '\f' => ret ++= "\\f"
      case '\\' => ret ++= "\\\\"
      case '\'' => ret ++= "\\\'"
      case '\"' => ret ++= "\\\""
      case x if canPrintLiterally(x) => ret += x
      case x => ret ++= "\\u%04X" format x.toInt
    }

    ret.result()
  }

  /**
    * Replaces all occurences of placeholders like `%(var)` to corresponding variables in `args` with respect to
    * specified `OutputFormat` (all placeholders will be escaped). `%` can be escaped as `%%`. Note: for performance
    * reasons this function will not use any `Map`s to index variables, it will use linear search every time it
    * encounters a variable.
    *
    * @param msg Interpolation string
    * @param args Interpolation variables
    * @param format Desired `OutputFormat` summoned implicitly
    * @tparam R Result type
    * @return Interpolation result wrapped by `OutputFormat`
    */
  def interpolate[R](msg: String, args: (String, Any)*)(implicit format: OutputFormat[R]): R = {
    val result = new StringBuilder

    var cursor = 0
    while (cursor < msg.length) {
      val pos = msg.indexOf(VariableCharacter, cursor)

      if (pos == -1) {
        result ++= msg.substring(cursor)
        cursor = Int.MaxValue
      } else {
        result ++= msg.substring(cursor, pos)

        if (pos + 1 >= msg.length)
          throw new IllegalArgumentException(s"Stray '$VariableCharacter' at the end of string")

        msg(pos + 1) match {
          case VariableCharacter =>
            result += VariableCharacter
            cursor = pos + 2

          case VariableParentheses._1 =>
            val end = msg.indexOf(VariableParentheses._2, pos + 2)

            if (end == -1)
              throw new IllegalArgumentException(s"Unterminated variable at $pos")

            val varName = msg.substring(pos + 2, end)
            if (varName.isEmpty)
              throw new IllegalArgumentException(s"Empty variable name at $pos")

            var idx = 0
            var found = false

            while (idx < args.size) {
              val v = args(idx)
              idx += 1
              if (v._1 == varName) {
                result ++= format.escape(v._2.toString)
                found = true
                idx = Int.MaxValue
              }
            }

            if (!found)
              throw new IllegalArgumentException(s"Undefined variable at $pos: '$varName'")

            cursor = end + 1

          case x => throw new InvalidInterpolationException(s"Invalid interpolation character after '$VariableCharacter': " +
                        s"'$x' (escape '$VariableCharacter' by typing it twice)")
        }
      }
    }

    format.convert(result.result())
  }

  /**
    * Extracts all referred variables from string and returns a `Set` with names.
    *
    * @param msg Interpolation string
    * @return Set of variable names referred in `msg`
    */
  def extractVariables(msg: String): Set[String] = {
    val result = Set.newBuilder[String]

    var cursor = 0
    while (cursor < msg.length) {
      val pos = msg.indexOf(VariableCharacter, cursor)

      if (pos == -1) cursor = Int.MaxValue
      else {
        if (pos + 1 >= msg.length)
          throw new IllegalArgumentException(s"Stray '$VariableCharacter' at the end of string")

        msg(pos + 1) match {
          case VariableCharacter =>
            cursor = pos + 2

          case VariableParentheses._1 =>
            val end = msg.indexOf(VariableParentheses._2, pos + 2)
            if (end == -1)
              throw new IllegalArgumentException(s"Unterminated variable at $pos")

            val varName = msg.substring(pos + 2, end)

            if (varName.isEmpty)
              throw new IllegalArgumentException("Empty variable name")

            result += varName
            cursor = end + 1

          case x => throw new InvalidInterpolationException(s"Invalid interpolation character after '$VariableCharacter': " +
            s"'$x' (escape '$VariableCharacter' by typing it twice)")
        }
      }
    }

    result.result()
  }
}
