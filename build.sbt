import sbt.Keys._

name := "scalingua-root"
version := "1.2.0-SNAPSHOT"
crossPaths := true

publishArtifact := false
publishTo := Some(Resolver.file("Transient repository", file("/tmp/unused")))

val common = Seq(
  organization := "ru.makkarpov",
  version := (LocalRootProject / version).value,

  crossPaths := true,
  scalaVersion := "2.12.18", //should be the same for all projects for cross-build to work
  crossScalaVersions := Seq(scalaVersion.value, "2.13.12"), //no support fo scala3 macros yet
  javacOptions ++= Seq( "-source", "1.8", "-target", "1.8" ),
  scalacOptions ++= Seq( "-Xfatal-warnings", "-feature", "-deprecation" ),

  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.17" % Test,

  Test / publishArtifact := false,
  Test / envVars := Map("SCALANATIVE_MIN_SIZE"-> "100m", "SCALANATIVE_MAX_SIZE"-> "100m"),
  publishMavenStyle := true,

  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/makkarpov/scalingua")),
  organizationName := "Maxim Karpov",
  organizationHomepage := Some(url("https://github.com/makkarpov")),
  scmInfo := Some(ScmInfo(
    browseUrl = url("https://github.com/makkarpov/scalingua"),
    connection = "scm:git://github.com/makkarpov/scalingua.git"
  )),

  // Seems that SBT key `developers` is producing incorrect results
  pomExtra := {
    <developers>
      <developer>
        <id>makkarpov</id>
        <name>Maxim Karpov</name>
        <url>https://github.com/makkarpov</url>
      </developer>
    </developers>
  },

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  // double publishes sbt artifact for some reason
  publishConfiguration := publishConfiguration.value.withOverwrite(true)
)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(common)
  .settings(
    name := "Scalingua Core",
    normalizedName := "scalingua-core",
    description := "A minimal set of runtime classes for Scalingua",

    libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2"
  )

lazy val scalingua = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .enablePlugins(ParserGenerator, AssemblyPlugin)
  .settings(common)
  .settings(
    name := "Scalingua",
    normalizedName := "scalingua",
    description := "A simple gettext-like internationalization library for Scala",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.github.vbmacher" % "java-cup-runtime" % "11b-20160615",
      "com.grack" % "nanojson" % "1.2"
    ),

    assembly / assemblyShadeRules := Seq(
      ShadeRule.rename("java_cup.runtime.**" -> "ru.makkarpov.scalingua.pofile.shaded_javacup.@1").inAll
    ),

    // include only CUP:
    assembly / assemblyExcludedJars := (assembly / fullClasspath).value.filterNot { f =>
      f.data.getName.contains("java-cup-runtime")
    }
  )
  .dependsOn(core)

lazy val scalingua_shadedCup = project.in(file("target/shaded-cup"))
    .settings(common)
    .settings(
      name := "Scalingua shaded",
      normalizedName := "scalingua-shaded",
      description := "Scalingua with shaded CUP runtime to prevent conflicts",

      Compile / packageBin := (scalingua.jvm / Compile / assembly).value,
      libraryDependencies := (scalingua.jvm / libraryDependencies).value.filterNot(_.name.contains("java-cup"))
    )
    .dependsOn(scalingua.jvm.dependencies:_*)

def playSettings = List(
  // Recent versions of Play supports only recent version of Scala
  //todo support scala3
  crossScalaVersions := Seq("2.13.12"),
  //workaround having to use same scalaversion for all projects for crossbuild to work
  libraryDependencies := (if (scalaVersion.value.startsWith("2.12")) Nil else libraryDependencies.value),
  (Compile / publishArtifact) := (if (scalaVersion.value.startsWith("2.12")) false else (Compile / publishArtifact).value),
  (Compile / sources) := (if (scalaVersion.value.startsWith("2.12")) Nil else (Compile / sources).value),
  (Test / sources) := (if (scalaVersion.value.startsWith("2.12")) Nil else (Test / sources).value),
  (Test / loadedTestFrameworks) := (if (scalaVersion.value.startsWith("2.12")) Map() else (Test / loadedTestFrameworks).value),
)

lazy val twirl = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(common)
  .settings(
    name := "Scalingua Twirl module",
    normalizedName := "scalingua-twirl",
    description := "An integration module for Twirl",
    libraryDependencies ++= Seq(
      "org.playframework.twirl" %%% "twirl-api" % "2.0.1",
    ),
  )
  .settings(playSettings)
  .dependsOn(scalingua)

lazy val play = project
  .settings(common)
  .settings(
    name := "Scalingua Play module",
    normalizedName := "scalingua-play",
    description := "An integration module for Play Framework",
    libraryDependencies ++= Seq(
      "org.playframework" %% "play" % "3.0.0"
    ),
  )
  .settings(playSettings)
  .dependsOn(twirl.jvm)

lazy val plugin = project
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .settings(common)
  .settings(
    name := "Scalingua SBT plugin",
    normalizedName := "scalingua-sbt",
    description := "SBT plugin that compiles locales, manages locations of *.pot files and so on",

    crossPaths := false,
    crossScalaVersions := Seq(scalaVersion.value), //sbt only runs on 2.12

    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M", "-XX:MaxPermSize=256M", "-Dscalingua.version=" + (LocalRootProject / version).value
    ),
    scriptedBufferLog := false,
    scripted := scripted.dependsOn(
      scalingua.jvm / publishLocal,
      core.jvm / publishLocal,
      scalingua.js / publishLocal,
      core.js / publishLocal).evaluated,
    pluginCrossBuild / sbtVersion := "1.2.8", //https://github.com/sbt/sbt/issues/5049
  ).dependsOn(scalingua.jvm)
