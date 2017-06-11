import org.scalatest
import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

class Test extends FlatSpec with Matchers {
  implicit val languageId = LanguageId("ru-RU")

  val subAMessages = Messages.compiled("subA")
  val subBMessages = Messages.compiled("subB")

  it should "translate SubA separately" in {
    implicit val messages = subAMessages

    SubA.testA shouldBe "Тест первый"
    SubA.testB shouldBe "Тест второй"
    SubB.testA shouldBe "Test A"
    SubB.testB shouldBe "Test B"
  }

  it should "translate SubB separately" in {
    implicit val messages = subBMessages

    SubA.testA shouldBe "Test A"
    SubA.testB shouldBe "Test B"
    SubB.testA shouldBe "Первый тест"
    SubB.testB shouldBe "Второй тест"
  }

  it should "translate merged messages" in {
    implicit val messages = subAMessages.merge(subBMessages)

    SubA.testA shouldBe "Тест первый"
    SubA.testB shouldBe "Тест второй"
    SubB.testA shouldBe "Первый тест"
    SubB.testB shouldBe "Второй тест"
  }
}