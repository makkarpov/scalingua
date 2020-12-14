package ru.makkarpov.scalingua.plugin

import java.io.{ByteArrayOutputStream, OutputStream}

sealed trait PoCompilerStrategy {

  def getInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String
}

object PoCompilerStrategy {
  def getStrategy(definition: String): PoCompilerStrategy = definition match {
    case "ReadFromResources" => new ReadFromResourcesStrategy
    case "InlineBase64" => new InlineBase64Strategy
    case _ => throw new IllegalArgumentException("Cannot create PoCompilerStrategy.")
  }
}

class ReadFromResourcesStrategy extends PoCompilerStrategy {
  override def getInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"""
       |  initialize({
       |    val str = getClass.getResourceAsStream("${ctx.filePrefix}data_${ctx.lang.language}_${ctx.lang.country}.bin")
       |    if (str eq null) {
       |      throw new IllegalArgumentException("Resource not found for language ${ctx.lang.language}_${ctx.lang.country}")
       |    }
       |    str
       |  })
    """.stripMargin
}

class InlineBase64Strategy extends PoCompilerStrategy {

  override def getInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String = {
    import java.util.Base64
    val javaStringLiteralLimit = 65535
    val out = new ByteArrayOutputStream()
    getStream(out)
    val str = Base64.getEncoder.encodeToString(out.toByteArray)
    val chunks = str.sliding(javaStringLiteralLimit, javaStringLiteralLimit).map(s => {
      s""""${s}""""
    }).mkString(",")

    val initializationBlock = {
      s"""
         |  val arr = Array($chunks)
         |
         |  initialize({
         |    import java.util.Base64
         |    import java.io.ByteArrayInputStream
         |    val decoded = Base64.getDecoder().decode(arr.mkString("").getBytes())
         |    val str = new ByteArrayInputStream(decoded)
         |    if (str eq null) {
         |      throw new IllegalArgumentException("Resource not found for language ${ctx.lang.language}_${ctx.lang.country}")
         |    }
         |    str
         |  })
    """.stripMargin
    }

    initializationBlock
  }
}


