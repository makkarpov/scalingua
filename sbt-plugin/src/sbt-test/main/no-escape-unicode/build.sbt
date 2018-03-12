enablePlugins(Scalingua)

resolvers += Resolver.defaultLocal
libraryDependencies ++= Seq(
  "ru.makkarpov" %% "scalingua" % {
    val v = System.getProperty("scalingua.version")
    if(v == null) throw new RuntimeException("Scalingua version is not defined")
    else v
  },
  "org.scalatest" %% "scalatest" % "2.2.6" % Test
)

localePackage in Test := "some.test.pkg"
escapeUnicode in Test := false