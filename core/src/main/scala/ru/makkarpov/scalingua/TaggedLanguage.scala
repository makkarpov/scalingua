package ru.makkarpov.scalingua

object TaggedLanguage {
  val Identity = new TaggedLanguage {
    override def taggedSingular(tag: String): String = tag
    override def taggedPlural(tag: String, n: Long): String = tag
  }
}

trait TaggedLanguage {
  /**
    * Returns singular string resolved by tag
    *
    * @param tag String tag
    * @return Resolved string
    */
  def taggedSingular(tag: String): String

  /**
    * Returns plural string resolved by tag with respect to quantity
    *
    * @param tag String tag
    * @param n Quantity
    * @return Resolved string
    */
  def taggedPlural(tag: String, n: Long): String
}
