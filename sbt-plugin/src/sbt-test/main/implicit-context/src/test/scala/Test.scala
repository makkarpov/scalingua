import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

class Test extends AnyFlatSpec with Matchers {
  implicit val messages = Messages.compiled("ru.makkarpov")
  implicit val languageId = LanguageId("ru-RU")

  it should "correctly translate strings" in {
    t"Hello, world!" shouldBe "Привет, мир!"

    tc("something", "Hello, world!") shouldBe "Привет, мир в контексте something!"
  }

  it should "include contextual strings in Messages" in {
    val lang = messages(languageId)

    lang.singular("Hello, world!") shouldBe "Hello, world!"
    lang.singular("ru.makkarpov", "Hello, world!") shouldBe "Привет, мир!"
    lang.singular("something", "Hello, world!") shouldBe "Hello, world!"
    lang.singular("ru.makkarpov:something", "Hello, world!") shouldBe "Привет, мир в контексте something!"
  }

  it should "reference contextual strings" in {
    implicit val mockLang = new Language {
      override def singular(msgctx: String, msgid: String): String = msgctx + "/" + msgid
      override def plural(msgid: String, msgidPlural: String, n: Long): String = fail
      override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String = fail
      override def singular(msgid: String): String = fail
      override def taggedSingular(tag: String): String = fail
      override def taggedPlural(tag: String, n: Long): String = fail

      override def merge(other: Language): Language = fail

      def fail = throw new IllegalArgumentException("Called an unexpected method")

      override def id: LanguageId = LanguageId("xx-XX")
    }

    t"Hello, world!" shouldBe "ru.makkarpov/Hello, world!"
    tc("context", "Hello, world!") shouldBe "ru.makkarpov:context/Hello, world!"
  }
}