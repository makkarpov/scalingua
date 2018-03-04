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

import sbt._
import Keys._
import ru.makkarpov.scalingua.{LanguageId, StringUtils}

object Scalingua extends AutoPlugin {

  object autoImport {
    val templateTarget = settingKey[File]("A location of *.pot file with scanned strings")

    val localePackage = settingKey[String]("A package with compiled locale files")

    val implicitContext = settingKey[Option[String]](
                                  "Context that will get implicitly added to the beginning of each message. "+
                                  "Useful to scope messages of whole project.")

    val includeImplicitContext = settingKey[Boolean]("Specifies whether to include implicit context in compiled messages")

    // Internal task just to make `resourceGenerators` happy. Does not have any settings,
    // uses them from `compileLocales` task.
    val packageLocales = taskKey[Seq[File]]("Compile *.po files to packed binary files")
    val compileLocales = taskKey[Seq[File]]("Compile *.po locales to Scala classes")
  }

  import autoImport._

  override def requires: Plugins = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings =
    inConfig(Compile)(localeSettings) ++ inConfig(Test)(localeSettings)

  def localeSettings = Seq(
    templateTarget := crossTarget.value / "messages" / (Defaults.nameForSrc(configuration.value.name) + ".pot"),
    localePackage := "locales",
    implicitContext := None,
    includeImplicitContext := true,

    includeFilter in compileLocales := "*.po",
    excludeFilter in compileLocales := HiddenFileFilter,
    sourceDirectories in compileLocales := Seq(sourceDirectory.value / "locales"),

    sources in compileLocales := Defaults.collectFiles(
      sourceDirectories in compileLocales,
      includeFilter in compileLocales,
      excludeFilter in compileLocales
    ).value,

    sources in packageLocales := (sources in compileLocales).value,

    target in compileLocales := crossTarget.value / "locales" / Defaults.nameForSrc(configuration.value.name) / "scala",
    target in packageLocales := crossTarget.value / "locales" / Defaults.nameForSrc(configuration.value.name) / "resources",

    compileLocales := compileLocalesTask.value,
    packageLocales := packageLocalesTask.value,

    sourceGenerators += compileLocales.taskValue,
    resourceGenerators += packageLocales.taskValue,

    managedSourceDirectories += (target in compileLocales).value,
    managedResourceDirectories += (target in packageLocales).value,

    scalacOptions += "-Xmacro-settings:scalingua:target=" + templateTarget.value.getCanonicalPath,
    scalacOptions += "-Xmacro-settings:scalingua:baseDir=" + longestCommonPrefix(sourceDirectories.value),
    scalacOptions ++= {
      implicitContext.value match {
        case Some(s) => Seq("-Xmacro-settings:scalingua:implicitContext=" + s)
        case None => Nil
      }
    }
  )

  private def longestCommonPrefix(s: Seq[File]) = s match {
    case Seq() => ""
    case Seq(head, tail @ _*) =>
      var current = head.getCanonicalPath
      for (f <- tail)
        current = current.zip(f.getCanonicalPath).takeWhile{ case (a, b) => a == b }.map(_._1).mkString
      current
  }

  private def createParent(f: File) = {
    val par = f.getParentFile
    if ((par ne null) && !par.isDirectory)
      IO.createDirectory(par)
  }

  private def filePkg(f: File, s: String) =
    if (s.isEmpty) f
    else f / s.replace('.', '/')

  def collectLangs(task: TaskKey[Seq[File]]) = Def.task {
    val langPattern = "^([a-z]{2})_([A-Z]{2})\\.po$".r
    val ret = Seq.newBuilder[LanguageId]

    for (src <- (sources in task).value) src.getName match {
      case langPattern(language, country) =>
        ret += LanguageId(language, country)

      case _ =>
        throw new IllegalArgumentException(s"Illegal file name '${src.getName}', should be formatted like 'en_US.po' (${src.getCanonicalPath})")
    }

    ret.result()
  }

  def withGenContext(task: TaskKey[Seq[File]], fileFormat: String)(f: GenerationContext => Unit) = Def.task {
    val baseTgt = (target in task).value
    val pkg = (localePackage in task).value
    val implicitCtx =
      if ((includeImplicitContext in task).value) (implicitContext in task).value.filter(_.nonEmpty)
      else None
    val log = streams.value.log

    val langPattern = "^([a-z]{2})_([A-Z]{2})\\.po$".r
    val ret = Seq.newBuilder[File]

    for (src <- (sources in task).value) src.getName match {
      case langPattern(language, country) =>
        val tgt = filePkg(baseTgt, pkg) / StringUtils.interpolate(fileFormat, "l" -> language, "c" -> country)
        createParent(tgt)

        val genCtx = GenerationContext(pkg, implicitCtx, LanguageId(language, country), src, tgt, log)
        try f(genCtx)
        catch {
          case p: ParseFailedException =>
            throw p

          case t: Throwable =>
            throw new IllegalArgumentException(s"Failed to compile ${src.getCanonicalPath}", t)
        }

        ret += tgt
      case _ =>
        throw new IllegalArgumentException(s"Illegal file name '${src.getName}', should be formatted like 'en_US.po' (${src.getCanonicalPath})")
    }

    ret.result()
  }

  def packageLocalesTask = withGenContext(packageLocales, "data_%(l)_%(c).bin")(PoCompiler.doPackaging)

  def compileLocalesTask = Def.task {
    val r = withGenContext(compileLocales, "Language_%(l)_%(c).scala")(PoCompiler.doCompiling).value

    val idx = {
      val langs = collectLangs(compileLocales).value
      val pkg = (localePackage in compileLocales).value

      val tgt = filePkg((target in compileLocales).value, pkg) / "Languages.scala"
      createParent(tgt)

      PoCompiler.generateIndex(pkg, tgt, langs)

      tgt
    }

    r :+ idx
  }
}
