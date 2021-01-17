package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.ComplexSymbolFactory.Location

case class ParserException(left: Location, right: Location, msg: String)
extends RuntimeException(s"at ${left.getUnit}:${left.getLine}:${left.getColumn}: $msg") {

}
