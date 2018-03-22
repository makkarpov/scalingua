logLevel := Level.Warn
resolvers += Resolver.url("bintray-sbt-plugins", url("https://dl.bintray.com/eed3si9n/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

libraryDependencies ++= Seq(
  "de.jflex" % "jflex" % "1.6.1",
  "com.github.vbmacher" % "java-cup" % "11b"
)