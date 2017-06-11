package ru.makkarpov.scalingua

import ru.makkarpov.scalingua.MergedLanguage.MessageData

/**
  * Created by makkarpov on 11.06.17.
  */
object MergedLanguage {
  case class MessageData(singular: Map[String, String], singularCtx: Map[(String, String), String],
                         plural: Map[String, Seq[String]], pluralCtx: Map[(String, String), Seq[String]]) {

    def merge(other: MessageData): MessageData =
      MessageData(other.singular ++ singular, other.singularCtx ++ singularCtx,
                  other.plural ++ plural, other.pluralCtx ++ pluralCtx)
  }
}

class MergedLanguage(val id: LanguageId, val data: MessageData, plural: PluralFunction) extends Language {
  /**
    * Resolve singular form of message without a context.
    *
    * @param msgid A message to resolve
    * @return Resolved message or `msgid` itself.
    */
  override def singular(msgid: String): String = data.singular.getOrElse(msgid, msgid)

  /**
    * Resolve singular form of message with a context.
    *
    * @param msgctx A context of message
    * @param msgid  A message to resolve
    * @return Resolved message or `msgid` itself.
    */
  override def singular(msgctx: String, msgid: String): String = data.singularCtx.getOrElse(msgctx -> msgid, msgid)

  /**
    * Resolve plural form of message without a context
    *
    * @param msgid       A singular form of message
    * @param msgidPlural A plural form of message
    * @param n           Numeral representing which form to choose
    * @return Resolved plural form of message
    */
  override def plural(msgid: String, msgidPlural: String, n: Long): String = data.plural.get(msgid) match {
    case Some(tr) => tr(plural.plural(n))
    case None => if (n == 1) msgid else msgidPlural
  }

  /**
    * Resolve plural form of message with a context.
    *
    * @param msgctx      A context of message
    * @param msgid       A singular form of message
    * @param msgidPlural A plural form of message
    * @param n           Numeral representing which form to choose
    * @return Resolved plural form of message
    */
  override def plural(msgctx: String, msgid: String, msgidPlural: String, n: Long): String =
    data.pluralCtx.get(msgctx -> msgid) match {
      case Some(tr) => tr(plural.plural(n))
      case None => if (n == 1) msgid else msgidPlural
    }

  /**
    * Merges this language with specified `other`. Conflicting messages will be resolved
    * using `this` language. Plural function will be used from `this` language.
    *
    * @param other Language to merge
    * @return Merged language
    */
  override def merge(other: Language): Language = other match {
    case ml: MergedLanguage => new MergedLanguage(id, data.merge(ml.data), plural)
    case cc: CompiledLanguage => new MergedLanguage(id, data.merge(cc.messageData), plural)
    case Language.English => other
    case _ => throw new NotImplementedError("Merge is supported only for MergedLanguage and CompiledLanguage")
  }
}
