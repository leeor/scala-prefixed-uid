val scala3Version = "3.1.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sc-uid",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.7",
    libraryDependencies += "com.monovore" %% "decline" % "2.2.0",
    libraryDependencies += "com.monovore" %% "decline-effect" % "2.2.0"
  )
