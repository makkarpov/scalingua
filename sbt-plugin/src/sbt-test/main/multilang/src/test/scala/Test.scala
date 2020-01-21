import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

// test whether Scalingua is able to compile messages for multiple languages at once:
class Test extends AnyFlatSpec with Matchers {
  implicit val messages = Messages.compiled()

  it should "provide messages for English" in {
    implicit val lang = LanguageId("en-US")

    t"Good evening!" shouldBe "Good evening!"
  }

  it should "provide messages for German" in {
    implicit val lang = LanguageId("de-DE")

    t"Good evening!" shouldBe "Guten Abend!"
  }

  it should "provide messages for Russian" in {
    implicit val lang = LanguageId("ru-RU")

    t"Good evening!" shouldBe "Добрый вечер!"
  }

  it should "provide messages for Chinese" in {
    implicit val lang = LanguageId("zh-ZH")

    t"Good evening!" shouldBe "晚上好！"
  }
}