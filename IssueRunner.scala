import sbt._, sbt.internal.util.AttributeMap
import Keys._
import complete.DefaultParsers._

import java.io.File

import scala.sys.process._
import scala.io.Source


object IssueRunner extends AutoPlugin with IssueRunnerImpl {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  val issuesWorkspaceAttr = AttributeKey[File]("issueWorkspace")

  def issuesWorkspace = Command.args("issuesWorkspace", "<workdir>") { case (state, dirName :: Nil) =>
    val newState = state.copy(attributes = state.attributes.put(issuesWorkspaceAttr, new File(dirName)))
    println(s"Issues Workspace is set to ${newState.attributes(issuesWorkspaceAttr)}")
    newState
  }

  def issue = Command.args("issue", "<dirName>, <args>") { case (state, dirName :: args) =>
    val issuesWorkspace = state.attributes(issuesWorkspaceAttr)
    val issueDir  = new File(issuesWorkspace, dirName)
    val launchSrc = locateLaunchFile(issueDir)

    val phases = List(
      scriptToSbtCommand,
      predefinedVars,
      scriptVars,
    )

    val ctx = Context(args, issueDir)
    val launchCmd = phases.foldLeft(Source.fromFile(launchSrc).mkString) { (src, phase) => phase(src, ctx) }

    println(s"Executing command:\n$launchCmd")
    Command.process(launchCmd, state)
  }

  override lazy val projectSettings = Seq(commands ++= Seq(issue, issuesWorkspace))
}


trait IssueRunnerImpl { this: IssueRunner.type =>
  type Phase = (String, Context) => String

  case class Context(args: List[String], issueDir: File)

  @annotation.tailrec final def locateLaunchFile(dir: File): File = {
    if (dir eq null) throw new RuntimeException("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    println(s"Attempting to load $launchFile")
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }

  val scriptToSbtCommand: Phase = (src, _) => src
    .split("\n")
    .map { line =>
      val commented = line.takeWhile(_ != '#')
      if (commented.headOption.filter(c => !c.isWhitespace).isDefined) s";$commented"
      else commented
    }
    .filter(line => !line.isEmpty && !line.forall(_.isWhitespace))
    .mkString

  val predefinedVars: Phase = (src, ctx) => src.replace("$here", ctx.issueDir.getPath)

  val scriptVars: Phase = (src, ctx) => {
    val pat = """\$(?<argid>\d+)""".r
    pat.replaceAllIn(src, m => ctx.args(m.group("argid").toInt - 1))
  }
}
