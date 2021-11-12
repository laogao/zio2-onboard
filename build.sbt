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

lazy val backend = project
  .in(file("backend"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.2.6" cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-stream" % "2.6.16" cross CrossVersion.for3Use2_13,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    //Compile / resourceGenerators += Def.task {
    //  val source = (frontend / Compile / scalaJSLinkedFile).value.data
    //  val dest = (Compile / resourceManaged).value / "assets" / "main.js"
    //  IO.copy(Seq(source -> dest))
    //  Seq(dest)
    //},
    run / fork := true
  )
  .dependsOn(shared.jvm)
