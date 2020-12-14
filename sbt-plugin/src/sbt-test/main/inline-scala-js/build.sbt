name := "inline-scala-js"

enablePlugins(ScalaJSPlugin, Scalingua)

libraryDependencies ++= Seq(
  "ru.makkarpov" %%% "scalingua" % {
    val v = System.getProperty("scalingua.version")
    if(v == null) throw new RuntimeException("Scalingua version is not defined")
    else v
  },
  "org.scalatest" %%% "scalatest" % "3.2.2" % Test
)

localePackage in Test := "some.test.pkg"
compileLocalesStrategy in Test := "InlineBase64"
scalaJSUseMainModuleInitializer := true
