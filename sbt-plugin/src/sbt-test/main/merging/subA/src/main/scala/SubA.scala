import ru.makkarpov.scalingua.Language
import ru.makkarpov.scalingua.I18n._

object SubA {
  def testA(implicit msgs: Language) = t"Test A"
  def testB(implicit msgs: Language) = t"Test B"
}