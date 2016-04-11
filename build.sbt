import sbt.Keys._

name := "scalingua-root"
version := "1.0"
crossPaths := true

enablePlugins(CrossPerProjectPlugin)

val common = Seq(
  organization := "ru.makkarpov",
  version := "0.1",

  crossPaths := true,
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.4", "2.11.7"),

  publishMavenStyle := true,
  pomExtra := {
    <url>https://github.com/makkarpov/scalingua</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>git://github.com/makkarpov/scalingua.git</url>
      <connection>scm:git://github.com/makkarpov/scalingua.git</connection>
    </scm>
    <developers>
      <developer>
        <id>makkarpov</id>
        <name>Maxim Karpov</name>
        <url>https://github.com/makkarpov</url>
      </developer>
    </developers>
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
    )
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