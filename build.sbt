import sbt.Keys._

name := "scalingua-root"
version := "0.4"
crossPaths := true

publishArtifact := false
publishTo := Some(Resolver.file("Transient repository", file("/tmp/unused")))

enablePlugins(CrossPerProjectPlugin)

val common = Seq(
  organization := "ru.makkarpov",
  version <<= version in LocalRootProject,

  crossPaths := true,
  scalaVersion := "2.10.4",
  crossScalaVersions := Seq("2.10.4", "2.11.7"),
  scalacOptions ++= Seq( "-Xfatal-warnings", "-feature", "-deprecation" ),

  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test,

  publishArtifact in Test := false,
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
  }
)

lazy val core = project
  .settings(common:_*)
  .settings(
    name := "Scalingua Core",
    normalizedName := "scalingua-core",
    description := "A minimal set of runtime classes for Scalingua"
  )

lazy val scalingua = project
  .settings(common:_*)
  .settings(
    name := "Scalingua",
    normalizedName := "scalingua",
    description := "A simple gettext-like internationalization library for Scala",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),

    libraryDependencies ++= {
      CrossVersion.binaryScalaVersion(scalaVersion.value) match {
        case "2.10" => Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
          "org.scalamacros" %% "quasiquotes" % "2.0.0"
        )
        case _ => Nil
      }
    }
  )
  .dependsOn(core)

lazy val play = project
  .settings(common:_*)
  .settings(
    name := "Scalingua Play! module",
    normalizedName := "scalingua-play",
    description := "An integration module for Play! Framework",

    // Recent versions of Play! supports only recent version of Scala.
    // We should keep `crossPath` to keep naming consistent
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq(scalaVersion.value),

    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "twirl-api" % "1.1.1",
      "com.typesafe.play" %% "play" % "2.5.0"
    )
  ).dependsOn(scalingua)

lazy val plugin = project
  .in(file("sbt-plugin"))
  .settings(common:_*)
  .settings(
    name := "Scalingua SBT plugin",
    normalizedName := "scalingua-sbt",
    description := "SBT plugin that compiles locales, manages locations of *.pot files and so on",

    crossPaths := false,
    scalaVersion := scala.util.Properties.versionNumberString,
    crossScalaVersions := Seq(scalaVersion.value),
    sbtPlugin := true,

    ScriptedPlugin.scriptedSettings,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M", "-XX:MaxPermSize=256M", "-Dscalingua.version=" + (version in LocalRootProject).value
    ),
    scriptedBufferLog := false
  ).dependsOn(scalingua)