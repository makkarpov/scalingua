package ru.makkarpov.scalingua.pofile

import java.io.StringReader
import java_cup.runtime.ComplexSymbolFactory

import parse.{ErrorReportingParser, PoLexer, PoParser, PoParserSym}

object ParserTest extends App {
  val text = "# comment\n#. ex comment\n#: test.scala:10\n#, fuzzy,qwe\n#~ some.tag\nmsgid \"ololo\"\nmsgstr \"test\\u00A7\"\n"

  println("INPUT:")

  println(text)

  println("LISTING TOKENS:")

  val lexer = new PoLexer(new StringReader(text))
  var token: java_cup.runtime.Symbol = _
  do {
    token = lexer.next_token()
    println(token)
  } while (token.sym != PoParserSym.EOF)

  println("PARSING:")

  val parser = new ErrorReportingParser(new PoLexer(new StringReader(text)))
  val ret = parser.parse()

  println(ret)
  println(s"value = ${ret.value}")
}
