package ru.makkarpov.scalingua.pofile

import java.io.{PrintWriter, Writer}

//uses system independent new lines
class NewLinePrintWriter(out: Writer, autoFlush: Boolean)
  extends PrintWriter(out, autoFlush) {
  def this(out: Writer) = this(out, false)

  override def println(): Unit = {
    print("\n")
    if (autoFlush) flush()
  }
}

