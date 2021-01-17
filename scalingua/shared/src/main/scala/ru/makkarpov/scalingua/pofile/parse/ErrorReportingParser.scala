package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.Symbol
import java_cup.runtime.ComplexSymbolFactory
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol

class ErrorReportingParser(lex: PoLexer)
  extends PoParser(lex, new ComplexSymbolFactory()) {

  private def report(pos: Symbol, msg: String): Unit = {
    if (pos.isInstanceOf[ComplexSymbol]) {
      throw new ParserException(pos.asInstanceOf[ComplexSymbol].xleft,
        pos.asInstanceOf[ComplexSymbol].xright,
        msg)
    } else {
      throw new RuntimeException(
        "Complex symbol expected for error reporting, got instead: " +
          pos)
    }
  }

  override def report_error(message: String, info: AnyRef): Unit = {
    report_fatal_error(message, info)
  }

  override def syntax_error(cur_token: Symbol): Unit = {
    unrecovered_syntax_error(cur_token)
  }

  override def unrecovered_syntax_error(cur_token: Symbol): Unit = {
    report(cur_token, "syntax error")
  }

  override def report_fatal_error(message: String, info: AnyRef): Unit = {
    if (info.isInstanceOf[ComplexSymbol]) {
      report(info.asInstanceOf[ComplexSymbol], message)
    } else {
      throw new RuntimeException(
        "Complex symbol expected for error reporting, got instead: " +
          info)
    }
  }

}
