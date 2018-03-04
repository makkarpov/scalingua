package ru.makkarpov.scalingua.pofile.parse

import java_cup.runtime.ComplexSymbolFactory.Location

import ru.makkarpov.scalingua.pofile.MultipartString

class MutablePlurals {
  private var _startLoc: Location = _
  private var _endLoc: Location = _
  private var _parts: Map[Int, MultipartString] = Map.empty

  def reset(): Unit = {
    _startLoc = null
    _parts = Map.empty
  }

  def add(n: Int, str: MultipartString, left: Location, right: Location): Unit = {
    if (_parts.contains(n))
      throw ParserException(left, right, s"duplicate plural message index: $n")

    if (_startLoc == null)
      _startLoc = left

    _endLoc = right

    _parts += n -> str
  }

  def result(): Seq[MultipartString] = {
    val cnt = _parts.size

    for (i <- 0 until cnt)
      if (!_parts.contains(i))
        throw ParserException(_startLoc, _endLoc, s"non-contiguous indices of plural strings: index $i is absent")

    (0 until cnt).map(_parts)
  }
}
