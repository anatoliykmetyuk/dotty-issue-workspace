lazy val root = (project in file(".")).
  settings(
    name := "dotty-workspace",
    version := "0.1.0",
    organization := "com.akmetiuk",
    scalaVersion := "2.12.8",
    sbtPlugin := true,

    libraryDependencies += "com.lihaoyi" %% "fansi" % "0.2.7",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.2" % "test",

    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
