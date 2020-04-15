package dotty.workspace
package core

import java.io.File
import java.util.Properties

import dotty.workspace.util.LogLevel


case class Context(
  args: List[String],
  issueName: String,
  properties: java.util.Properties,
) {
  lazy val workspaceDir = new File(properties.getProperty("workspace_path"))

  lazy val logLevel =
    Option(properties.getProperty("log_level"))
      .map(LogLevel(_)).getOrElse(LogLevel.Info)

  lazy val issueDir = new File(workspaceDir, issueName)

  lazy val launchFile = locateLaunchFile(issueDir)

  @annotation.tailrec
  private final def locateLaunchFile(dir: File): File = {
    if (dir eq null) fail("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    info(s"Attempting to load $launchFile")(logLevel)
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }

}
