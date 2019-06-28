package ru.makkarpov.scalingua.pofile.parse;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;

public class ErrorReportingParser extends PoParser {
  public ErrorReportingParser(PoLexer lex) {
    super(lex, new ComplexSymbolFactory());
  }

  private void report(Symbol pos, String msg) {
    if (pos instanceof ComplexSymbol) {
      throw new ParserException(((ComplexSymbol) pos).xleft, ((ComplexSymbol) pos).xright, msg);
    } else {
      throw new RuntimeException("Complex symbol expected for error reporting, got instead: " + pos);
    }
  }

  @Override
  public void report_error(String message, Object info) {
    report_fatal_error(message, info);
  }

  @Override
  public void syntax_error(Symbol cur_token) {
    unrecovered_syntax_error(cur_token);
  }

  @Override
  public void unrecovered_syntax_error(Symbol cur_token) {
    report(cur_token, "syntax error");
  }

  @Override
  public void report_fatal_error(String message, Object info) {
    if (info instanceof ComplexSymbol) {
      report((ComplexSymbol)info, message);
    } else {
      throw new RuntimeException("Complex symbol expected for error reporting, got instead: " + info);
    }
  }

}
