import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

class Test extends AnyFlatSpec with Matchers {
  implicit val messages = Messages.compiled("some.test.pkg")
  implicit val langId = LanguageId("en-US")

  it should "disable escaping of unicode" in {
    t("Hello, world!") shouldBe "Hello, world!"
    t("Привет, мир!") shouldBe "Привет, мир!"
    t"Weird’quotes" shouldBe "Weird’quotes"
  }
}