package dotty.workspace

import sbt.{ Command => SBTCommandAPI, _ }
import Keys._
import complete.DefaultParsers._

import java.io.File
import scala.io.Source

import dotty.workspace.core._


object IssueRunner extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  def issue = SBTCommandAPI.args("issue", "<dirName>, <args>") { case (initialState, dirName :: args) =>
    val issuesWorkspace = new File(Source.fromFile(s"${sys.props("user.home")}/.sbt/1.0/plugins/dotty-workspace-path").mkString.replace("\n", ""))
    val issueDir  = new File(issuesWorkspace, dirName)
    val launchSrc = locateLaunchFile(issueDir)

    implicit val ctx = Context(args, issueDir)
    val src = Source.fromFile(launchSrc).mkString
    val launchCmds = Compiler.compile(src)
    Interpreter.run(launchCmds, initialState, ctx.issueDir)
  }

  @annotation.tailrec
  private final def locateLaunchFile(dir: File): File = {
    if (dir eq null) throw new RuntimeException("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    println(s"Attempting to load $launchFile")
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }

  override lazy val projectSettings = Seq(commands += issue)
}
