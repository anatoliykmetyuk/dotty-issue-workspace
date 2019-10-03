import sbt._, sbt.internal.util.AttributeMap
import Keys._
import complete.DefaultParsers._

import java.io.File

import scala.sys.process._
import scala.io.Source


object IssueRunner extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  val issuesWorkspaceAttr = AttributeKey[File]("issueWorkspace")

  def issuesWorkspace = Command.args("issuesWorkspace", "<workdir>") { case (state, dirName :: Nil) =>
    val newState = state.copy(attributes = state.attributes.put(issuesWorkspaceAttr, new File(dirName)))
    println(s"Issues Workspace is set to ${newState.attributes(issuesWorkspaceAttr)}")
    newState
  }

  @annotation.tailrec def locateLaunchFile(dir: File): File = {
    if (dir eq null) throw new RuntimeException("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    println(s"Attempting to load $launchFile")
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }

  def issue = Command.args("issue", "<dirName>") { case (state, dirName :: Nil) =>
    val issuesWorkspace = state.attributes(issuesWorkspaceAttr)
    val issueDir  = new File(issuesWorkspace, dirName)
    val launchSrc = locateLaunchFile(issueDir)

    val launchCmd = Source.fromFile(launchSrc).getLines
      .map { line =>
        val commented = line.takeWhile(_ != '#')
        if (commented.headOption.filter(c => !c.isWhitespace).isDefined) s";$commented"
        else commented
      }
      .filter(line => !line.isEmpty && !line.forall(_.isWhitespace))
      .mkString
      .replace("$here", issueDir.getPath)

    println(s"Executing command:\n$launchCmd")
    Command.process(launchCmd, state)
  }

  override lazy val projectSettings = Seq(commands ++= Seq(issue, issuesWorkspace))
}
