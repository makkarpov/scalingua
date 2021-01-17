/******************************************************************************
 * Copyright © 2016 Maxim Karpov                                              *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package ru.makkarpov.scalingua.plugin

import java.io.{ByteArrayOutputStream, OutputStream}

sealed trait PoCompilerStrategy {
  /** Specifies if plugin must generate Languages object indexing available languages. */
  def generatesIndex: Boolean = true

  /** Specifies if plugin must package *.po files into binary files. */
  def isPackagingNecessary: Boolean = true

  def getEnglishTagsDefinition(EnglishTagsClass: String): String = s"object $EnglishTagsClass"

  def getEnglishTagsInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String

  def getCompiledLanguageDefinition(ctx: GenerationContext): String =
    s"object Language_${ctx.lang.language}_${ctx.lang.country}"

  def getCompiledLanguageInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String
}

object PoCompilerStrategy {
  def getStrategy(definition: String): PoCompilerStrategy = definition match {
    case "ReadFromResources" => new ReadFromResourcesStrategy
    case "InlineBase64" => new InlineBase64Strategy
    case "LoadInRuntime" => new LoadInRuntimeStrategy
    case _ => throw new IllegalArgumentException("Cannot create PoCompilerStrategy.")
  }
}

class ReadFromResourcesStrategy extends PoCompilerStrategy {
  override def getEnglishTagsInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"""
       |  initialize({
       |    val str = getClass.getResourceAsStream("${ctx.filePrefix}compiled_english_tags.bin")
       |    if (str eq null)
       |      throw new NullPointerException("Compiled english tags not found!")
       |    str
       |  })
         """.stripMargin

  override def getCompiledLanguageInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
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

  import InlineBase64Strategy._

  override val isPackagingNecessary: Boolean = false

  override def getEnglishTagsInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"""
       |  val arr = ${inlineBase64Arr(getStream)}
       |
       |  initialize({
       |    import java.util.Base64
       |    import java.io.ByteArrayInputStream
       |    val decoded = Base64.getDecoder().decode(arr.mkString("").getBytes())
       |    val str = new ByteArrayInputStream(decoded)
       |    if (str eq null)
       |      throw new NullPointerException("Compiled english tags not found!")
       |    str
       |  })
         """.stripMargin

  override def getCompiledLanguageInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"""
       |  val arr = ${inlineBase64Arr(getStream)}
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

object InlineBase64Strategy {

  import java.util.Base64

  private val JavaStringLiteralLimit = 65535

  private def inlineBase64Arr(getStream: OutputStream => Unit): String = {
    val out = new ByteArrayOutputStream()
    getStream(out)
    val str = Base64.getEncoder.encodeToString(out.toByteArray)
    val arrElements = str.sliding(JavaStringLiteralLimit, JavaStringLiteralLimit).map(s => {
      s""""${s}""""
    }).mkString(",")
    s"Array($arrElements)"
  }
}

class LoadInRuntimeStrategy extends PoCompilerStrategy {
  override def generatesIndex: Boolean = false

  override def getEnglishTagsDefinition(EnglishTagsClass: String): String = s"class ${EnglishTagsClass}(is: java.io.InputStream)"

  override def getEnglishTagsInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"initialize(is)"

  override def getCompiledLanguageDefinition(ctx: GenerationContext): String =
    s"class Language_${ctx.lang.language}_${ctx.lang.country}(is: java.io.InputStream)"

  override def getCompiledLanguageInitializationBlock(ctx: GenerationContext, getStream: OutputStream => Unit): String =
    s"initialize(is)"
}
