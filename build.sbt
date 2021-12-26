ThisBuild / scalaVersion := "3.1.0"

lazy val quill = project
  .in(file("quill"))
  .settings(
    scalaVersion := "2.13.7",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.13",
      "io.getquill" %% "quill-jdbc-zio" % "3.12.0",
      "org.postgresql" % "postgresql" % "42.3.1",
      "dev.zio" %% "zio-macros" % "1.0.12",
      "io.github.kitlangton" %% "zio-magic" % "0.3.11",
      "org.flywaydb" % "flyway-core" % "8.2.3",
      "com.typesafe" % "config" % "1.4.1"
    ),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % "0.2.0-M3",
      "io.circe" %%% "circe-generic" % "0.14.1",
      "io.circe" %%% "circe-parser" % "0.14.1",
      "org.scalameta" %%% "munit" % "0.7.29" % Test
    )
  )

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "0.14.2",
      "org.scala-js" %%% "scalajs-dom" % "2.0.0"
    )
  )
  .dependsOn(shared.js)

lazy val backend = project
  .in(file("backend"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.2.6" cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-stream" % "2.6.17" cross CrossVersion.for3Use2_13,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    Compile / resourceGenerators += Def.task {
      val source = (frontend / Compile / scalaJSLinkedFile).value.data
      val dest = (Compile / resourceManaged).value / "assets" / "main.js"
      IO.copy(Seq(source -> dest))
      Seq(dest)
    },
    run / fork := true
  )
  .dependsOn(shared.jvm)
