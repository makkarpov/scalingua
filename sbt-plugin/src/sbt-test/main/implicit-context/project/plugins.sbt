addSbtPlugin("ru.makkarpov" % "scalingua-sbt" % {
  val ver = System.getProperty("scalingua.version")
  if(ver == null) throw new RuntimeException("Scalingua version is not defined")
  ver
})
