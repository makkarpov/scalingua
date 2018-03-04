package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.Symbol
import java_cup.runtime.ComplexSymbolFactory
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol

class ErrorReportingParser(lex: PoLexer) extends PoParser(lex, new ComplexSymbolFactory) {
  private def report(pos: Symbol, msg: String): Unit = pos match {
    case cs: ComplexSymbol => throw ParserException(cs.xleft, cs.xright, msg)
    case _ => throw new RuntimeException("Complex symbol expected for error reporting, got instead: " + pos)
  }

  override def report_error(message: String, info: AnyRef): Unit = report_fatal_error(message, info)

  override def syntax_error(cur_token: Symbol): Unit = unrecovered_syntax_error(cur_token)

  override def unrecovered_syntax_error(cur_token: Symbol): Unit = report(cur_token, s"syntax error")

  override def report_fatal_error(message: String, info: AnyRef): Unit = info match {
    case cs: ComplexSymbol => report(cs, message)
    case _ => throw new RuntimeException("Complex symbol expected for error reporting, got instead: " + info)
  }
}
