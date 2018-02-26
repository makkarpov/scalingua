import org.scalatest
import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.{LanguageId, Messages, Language}
import ru.makkarpov.scalingua.I18n._

class Test extends FlatSpec with Matchers {
  implicit val messages = Messages.compiled("some.test.pkg")

  it should "provide correct messages for en_US" in {
    implicit val langId = LanguageId("en-US")

    t("Hello, world!") shouldBe "Hello, world!"
    p("There is %(n) dog!", "There is %(n) dogs!", 7) shouldBe "There is 7 dogs!"
  }

  it should "provide correct messages for ru_RU" in {
    implicit val langId = LanguageId("ru-RU")

    t("Hello, world!") shouldBe "Привет, мир!"
    p("There is %(n) dog!", "There is %(n) dogs!", 7) shouldBe "Здесь 7 собак!"
  }

  it should "provide english messages for absent languages" in {
    implicit val langId = LanguageId("xx-QQ")

    t("Hello, world!") shouldBe "Hello, world!"
    p("There is %(n) dog!", "There is %(n) dogs!", 7) shouldBe "There is 7 dogs!"
  }

  it should "provide correct messages for other countries (ru_XX)" in {
    implicit val langId = LanguageId("ru-XX")

    t("Hello, world!") shouldBe "Привет, мир!"
    p("There is %(n) dog!", "There is %(n) dogs!", 7) shouldBe "Здесь 7 собак!"
  }

  it should "provide correct messages for generic languages (ru)" in {
    implicit val langId = LanguageId("ru")

    t("Hello, world!") shouldBe "Привет, мир!"
    p("There is %(n) dog!", "There is %(n) dogs!", 7) shouldBe "Здесь 7 собак!"
  }

  it should "handle percent signs" in {
    implicit val langId = LanguageId("ru-RU")

    t"Percents! %" shouldBe "Проценты! %"
    t("Percents!! %%") shouldBe "Проценты!! %"

    val x = 1
    t"Percents with variables%: $x, percents%" shouldBe s"Проценты с перменными%: $x, проценты%"

    t"Percents after variable: $x%%" shouldBe s"Проценты после переменной: $x%"

    // Plural:
    p"Look, I have $x percent${S.s}: %!" shouldBe s"Смотри, у меня $x процент: %!"
  }

  it should "reject invalid percent signs" in {
    """
      val x = 123
      t"Test: $x% qweqwe"
    """ shouldNot typeCheck
  }
}