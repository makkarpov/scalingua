logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")

libraryDependencies ++= Seq(
  "de.jflex" % "jflex" % "1.6.1",
  "com.github.vbmacher" % "java-cup" % "11b"
)