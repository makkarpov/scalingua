import org.scalatest
import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

class Test extends FlatSpec with Matchers {
  implicit val messages = Messages.compiled("some.test.pkg")

  it should "provide correct messages for en_US" in {
    implicit val languageId = LanguageId("en-US")

    t"Test" shouldBe "Test"
    tag("test.key") shouldBe "test message"
    ptag("test.plural.key", 1) shouldBe "singular form"
    ptag("test.plural.key", 2) shouldBe "plural forms"
  }

  it should "provide correct messages for ru_RU" in {
    implicit val languageId = LanguageId("ru-RU")

    t"Test" shouldBe "Тест"
    tag("test.key") shouldBe "Тестовое сообщение"
    ptag("test.plural.key", 1) shouldBe "Единственное число"
    ptag("test.plural.key", 2) shouldBe "Почти единственное число"
    ptag("test.plural.key", 5) shouldBe "Множественное число"
  }

  it should "provide correct messages for lazy strings" in {
    val l = lptag("test.plural.key", 2)

    {
      implicit val id = LanguageId("ru-RU")
      l.resolve shouldBe "Почти единственное число"
    }

    {
      implicit val id = LanguageId("en-US")
      l.resolve shouldBe "plural forms"
    }

    {
      implicit val id = LanguageId("xx-YY")
      l.resolve shouldBe "plural forms"
    }
  }
}