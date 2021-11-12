ThisBuild / scalaVersion := "3.0.2"

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % "0.14.1",
      "io.circe" %%% "circe-parser" % "0.14.1",
      "org.scalameta" %%% "munit" % "0.7.29" % Test
    )
  )
