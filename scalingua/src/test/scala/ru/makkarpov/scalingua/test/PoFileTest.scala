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

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.pofile._

class PoFileTest extends FlatSpec with Matchers {
  def t(data: String): Seq[Message] = {
    val bais = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
    PoFile(bais).toList
  }

  it should "handle files consisting only of whitespace" in {
    t("") shouldBe Seq.empty
    t("   ") shouldBe Seq.empty
    t("""
        |
      """.stripMargin) shouldBe Seq.empty
  }

  it should "handle files consisting only of whitespace and comments" in {
    t("#  123") shouldBe Seq.empty
    t("""# 123
        |#: test.scala:12
        |#, fuzzy
        |#. translator comment
        |# other comment
        |#: otherfile.scala:25
        |#. other tr comment
      """.stripMargin) shouldBe Seq.empty
    t("""# 123
        |
        |
        |#: test.scala:25
        |
        |#, fuzzy
        |
        |
        |
        |#. tr comment
      """.stripMargin) shouldBe Seq.empty
  }

  it should "discard incorrect header entries" in {
    an [IllegalArgumentException] shouldBe thrownBy(t("#!123"))
    an [IllegalArgumentException] shouldBe thrownBy(t("#, wtfflag"))
    an [IllegalArgumentException] shouldBe thrownBy(t("#"))
    an [IllegalArgumentException] shouldBe thrownBy(t("#: test.scala")) // without line
  }

  it should "parse singular forms" in {
    t("""#  comment
        |#: file.scala:20
        |#, fuzzy
        |#. tr comment
        |msgid "test"
        |msgstr "1234"
        |
        |msgid "1234"
        |msgstr "test"
        |msgid "escapes"
        |msgstr "\u0000\n\t\r"
      """.stripMargin) shouldBe Seq(
        Message.Singular(
          header = MessageHeader(
            Seq("comment"),
            Seq("tr comment"),
            Seq(MessageLocation("file.scala", 20)),
            MessageFlag.ValueSet(MessageFlag.Fuzzy)
          ),
          context = None,
          message = MultipartString("test"),
          translation = MultipartString("1234")
        ),
        Message.Singular(
          header = MessageHeader(Nil, Nil, Nil, MessageFlag.ValueSet.empty),
          context = None,
          message = MultipartString("1234"),
          translation = MultipartString("test")
        ),
        Message.Singular(
          header = MessageHeader(Nil, Nil, Nil, MessageFlag.ValueSet.empty),
          context = None,
          message = MultipartString("escapes"),
          translation = MultipartString("\u0000\n\t\r")
        )
      )
  }

  it should "parse multi-line strings" in {
    t("""# A PoFile header:
        |msgid ""
        |msgstr ""
        |"Header-Entry: value\n"
        |"Other-Header: other-value\n"
        |
        |"Something: better\n"
        |
        |
        |
        |"Than: just gettext\n"
      """.stripMargin) shouldBe Seq(
        Message.Singular(
          header = MessageHeader(Seq("A PoFile header:"), Nil, Nil, MessageFlag.ValueSet.empty),
          context = None,
          message = MultipartString(""),
          translation = MultipartString(
            "",
            "Header-Entry: value\n",
            "Other-Header: other-value\n",
            "Something: better\n",
            "Than: just gettext\n"
          )
        )
      )
  }

  it should "parse contextual singular forms" in {
    t("""# Header
        |    msgctxt "con"
        | "text"
        |     msgid "123"
        |  msgstr "qwe"
      """.stripMargin) shouldBe Seq(
        Message.Singular(
          header = MessageHeader(Seq("Header"), Nil, Nil, MessageFlag.ValueSet.empty),
          context = Some(MultipartString("con", "text")),
          message = MultipartString("123"),
          translation = MultipartString("qwe")
        )
      )
  }

  it should "parse plural forms" in {
    t("""msgid "digit"
        |msgid_plural "digits"
        |msgstr[0] "цыфра"
        |msgstr[1] "цыфры"
        |msgstr[2] "цыфир"
        |
        |msgctxt "1"
        |msgid "2"
        |msgid_plural "3"
        |msgstr[0] "4"
      """.stripMargin) shouldBe Seq(
        Message.Plural(
          header = MessageHeader(Nil, Nil, Nil, MessageFlag.ValueSet.empty),
          context = None,
          message = MultipartString("digit"),
          plural = MultipartString("digits"),
          translations = Seq(
            MultipartString("цыфра"),
            MultipartString("цыфры"),
            MultipartString("цыфир")
          )
        ),
        Message.Plural(
          header = MessageHeader(Nil, Nil, Nil, MessageFlag.ValueSet.empty),
          context = Some(MultipartString("1")),
          message = MultipartString("2"),
          plural = MultipartString("3"),
          translations = Seq(
            MultipartString("4")
          )
        )
      )
  }

  it should "discard invalid declarations" in {
    an [IllegalArgumentException] shouldBe thrownBy(t(""" pig "" """))
    an [IllegalArgumentException] shouldBe thrownBy(t(""" msgid "" """))
    an [IllegalArgumentException] shouldBe thrownBy(t(""" msgstr "" """))
    an [IllegalArgumentException] shouldBe thrownBy(t(""" msgctxt "" """))
    an [IllegalArgumentException] shouldBe thrownBy(t(""" msgstr[0] "" """))

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid "123"
          |#  Comment
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgctxt ""
          |msgid ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgstr ""
          |msgctxt ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgid_plural ""
          |msgstr ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgstr[0] ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgid_plural ""
          |msgstr[1] ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgid_plural ""
          |msgstr[0] ""
          |msgstr[2] ""
        """.stripMargin)
    }

    an [IllegalArgumentException] shouldBe thrownBy {
      t("""msgid ""
          |msgid_plural ""
          |msgstr[1] ""
          |msgstr[0] ""
        """.stripMargin)
    }
  }

  it should "parse complex examples" in {
    t(
      """#: test.scala:10
        |#: file.scala:20
        |#  A sample string
        |#. Should be translated
        |#, fuzzy
        |msgid "Dog"
        |msgid_plural "Dogs"
        |msgstr[0] "собака"
        |msgstr[1] "собаки"
        |msgstr[2] "собак"
        |
        |#: x.scala:25
        |msgid "Hello, world!"
        |msgstr "Привет, мир!"
        |
        |msgctxt "testing"
        |msgid "lol"
        |msgstr "123"
        |
        |# Trailing header
      """.stripMargin) shouldBe Seq(
        Message.Plural(
          header = MessageHeader(
            comments = Seq("A sample string"),
            extractedComments = Seq("Should be translated"),
            locations = Seq( MessageLocation("test.scala", 10), MessageLocation("file.scala", 20) ),
            flags = MessageFlag.ValueSet(MessageFlag.Fuzzy)
          ),
          context = None,
          message = MultipartString("Dog"),
          plural = MultipartString("Dogs"),
          translations = Seq(
            MultipartString("собака"),
            MultipartString("собаки"),
            MultipartString("собак")
          )
        ),
        Message.Singular(
          header = MessageHeader(Nil, Nil, Seq( MessageLocation("x.scala", 25) ), MessageFlag.ValueSet.empty),
          context = None,
          message = MultipartString("Hello, world!"),
          translation = MultipartString("Привет, мир!")
        ),
        Message.Singular(
          header = MessageHeader(Nil, Nil, Nil, MessageFlag.ValueSet.empty),
          context = Some(MultipartString("testing")),
          message = MultipartString("lol"),
          translation = MultipartString("123")
        )
      )
  }
}