import sbt.Keys._

name := "scalingua-root"
version := "1.0"
crossPaths := true

publishArtifact := false
publishTo := Some(Resolver.file("Transient repository", file("/tmp/unused")))

enablePlugins(CrossPerProjectPlugin)

val common = Seq(
  organization := "ru.makkarpov",
  version := "0.2",

  crossPaths := true,
  scalaVersion := "2.10.4",
  crossScalaVersions := Seq("2.10.4", "2.11.7"),
  scalacOptions ++= Seq( "-Xfatal-warnings", "-feature", "-deprecation" ),

  publishArtifact in Test := false,
  publishMavenStyle := true,

  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/makkarpov/scalingua")),
  scmInfo := Some(ScmInfo(
    browseUrl = new URL("https://github.com/makkarpov/scalingua"),
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
    name := "scalingua-core"
  )

lazy val scalingua = project
  .settings(common:_*)
  .settings(
    name := "scalingua",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scalatest" %% "scalatest" % "2.2.6" % Test
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

lazy val plugin = project
  .in(file("sbt-plugin"))
  .settings(common:_*)
  .settings(
    name := "scalingua-sbt",

    crossPaths := false,
    scalaVersion := scala.util.Properties.versionNumberString,
    crossScalaVersions := Seq(scalaVersion.value),
    sbtPlugin := true
  )
  .dependsOn(scalingua)