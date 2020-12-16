package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.ComplexSymbolFactory.Location

case class LexerException(loc: Location, msg: String)
extends RuntimeException(s"at ${loc.getUnit}:${loc.getLine}:${loc.getColumn}: $msg") {

}
