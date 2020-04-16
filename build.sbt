lazy val publishingSettings = List(
  organizationHomepage := Some(url("https://akmetiuk.com/")),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/anatoliykmetyuk/dotty-issue-workspace"),
      "scm:git@github.com:anatoliykmetyuk/dotty-issue-workspace.git"
    )
  ),

  developers := List(
    Developer(
      id    = "anatoliykmetyuk",
      name  = "Anatolii Kmetiuk",
      email = "anatoliykmetyuk@gmail.com",
      url   = url("https://akmetiuk.com")
    )
  ),

  description := "An SBT-based build tool for Dotty issue reproduction",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/anatoliykmetyuk/dotty-issue-workspace")),

  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value,

  credentials ++= (
    for {
      username <- sys.env.get("SONATYPE_USER")
      password <- sys.env.get("SONATYPE_PW")
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)
  ).toList,

  Global / PgpKeys.gpgCommand := (baseDirectory.value / "project/scripts/gpg.sh").getAbsolutePath,
)

lazy val root = (project in file("."))
  .settings(publishingSettings)
  .settings(
    name := "dotty-workspace",
    version := "0.1.0",
    organization := "com.akmetiuk",
    scalaVersion := "2.12.8",
    sbtPlugin := true,

    libraryDependencies += "com.lihaoyi" %% "fansi" % "0.2.7",
    libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.2" % "test",

    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
