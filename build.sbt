lazy val root = (project in file(".")).
  settings(
    name := "dotty-workspace",
    version := "0.1.0",
    organization := "com.akmetiuk",
    scalaVersion := "2.12.8",
    sbtPlugin := true,
  )
