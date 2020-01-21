resolvers in ThisBuild += Resolver.defaultLocal
libraryDependencies in ThisBuild ++= Seq(
  "ru.makkarpov" %% "scalingua" % {
    val v = System.getProperty("scalingua.version")
    if(v == null) throw new RuntimeException("Scalingua version is not defined")
    else v
  },
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)

lazy val subA = project
  .enablePlugins(Scalingua)
  .settings(
    localePackage in Compile := "subA",
    implicitContext in Compile := Some("subA")
  )

lazy val subB = project
  .enablePlugins(Scalingua)
  .settings(
    localePackage in Compile := "subB",
    implicitContext in Compile := Some("subB")
  )

lazy val root = project.in(file(".")).dependsOn(subA, subB)