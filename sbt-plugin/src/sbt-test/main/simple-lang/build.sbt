enablePlugins(Scalingua)

resolvers += Resolver.defaultLocal
libraryDependencies ++= Seq(
  "ru.makkarpov" %% "scalingua" % {
    val v = System.getProperty("scalingua.version")
    if(v == null) throw new RuntimeException("Scalingua version is not defined")
    else v
  },
  "org.scalatest" %% "scalatest" % "3.0.7" % Test
)

localePackage in Test := "some.test.pkg"