import sbt.{ Command => SBTCommandAPI, _ }, sbt.internal.util.AttributeMap
import Keys._
import complete.DefaultParsers._

import java.io.File

import scala.sys.process._
import scala.io.Source


sealed trait Command
case class SbtCommand(cmd: String) extends Command
case class ShellCommand(cmd: String) extends Command

case class Context(args: List[String], issueDir: File)

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
      makeStatements,     // Transform a script string into a sequence of statements
      substituteVars,     // Traverse statements, for each, substitute $var with the value of that variable
      makeCommands,       // Generate a Commands tree node with the list of commands for SBT and Shell
      normalizeClasspath, // For each generated SBT command, remove whitespaces around classpath delimiters
    )

    val ctx = Context(args, issueDir)
    val src: Tree = RawScript(Source.fromFile(launchSrc).mkString)
    val launchCmds): Tree = phases.foldLeft(src) {
      (src, phase) => phase(src, ctx) }

    launchCmds match {
      case Commands(cmds) =>
        for (cmd <- cmds) cmd match {
          case SbtCommand(cmd) =>
            println(s"Executing SBT command:\n$cmd")
            Command.process(launchCmd, state)

          case ShellCommand(cmd) =>
            println(s"Executing shell command:\n$cmd")
            exec(cmd)
        }

      case x => throw new RuntimeException(
        s"Expected a list of commands after issue script processing, got: $x")
    }


  }

  override lazy val projectSettings = Seq(commands ++= Seq(issue, issuesWorkspace))
}

trait IssueRunnerPhases { this: IssueRunner.type =>
  val valNamePat = """[\w\-_\d]+"""

  val makeStatements: Phase = { case (RawScript(src), _) =>
    val lines = src.split("\n")
    val stats = ListBuffer.empty[Statement]
    var currentStat: Statement = null

    def removeComments(line: String): String =
      line.takeWhile(_ != '#')

    def finalizeMultilineStatement(line: String): Unit = {
      stats += currentStat
      currentStat = null
    }

    def makeVal(line: String): ValDef = {
      val pat = s"""val\s+($namePat)\s+=\s+(.+)""".r
      line match {
        case pat(name, value) => ValDef(name, value)
      }
    }

    def makeShellScript(line: String): ShellCommand = {
      val pat = """\$ (.+)""".r
      line match {
        case pat(cmd) => ShellCommand(cmd)
      }
    }

    def appendToCurrentStatement(line: String): Unit = {
      val pat = """\s+(.+)""".r
      line match {
        case pat(str) => currentStat = currentStat.append(line)
      }
    }

    def makeSbtCommand(line: String): SbtCommand = SbtCommand(line)

    for {
      lineRaw <- lines
      line = removeComments(rawLine)
      if line.nonEmpty && !line.forAll(_.isWhitespace)
    } {
      if (line.startsWith(" ")) appendToCurrentStatement(line)
      else {
        if (currentStat != null) finalizeMultilineStatement()
        currentStat =
          if (line.startsWith("val")) makeVal(line)
          else if (line.startsWith("$")) makeShellScript(line)
          else makeSbtCommand(line)
      }
    }

    if (currentStat != null) finalizeMultilineStatement()
    Statements(stats.toList)
  }

  val substituteVars: Phase = { case (Statements(stats), ctx) =>
    val variables = mutable.Map[String, String]("here" -> ctx.issueDir.getPath)
    for { (arg, id) <- ctx.args.zipWithIndex }
      variables.updated((id + 1).toString, arg)

    stats.map {
      case ValDef(name, value) => variables.update(name, value)
      case stat: Command =>
        val valReferencePat = Regex("\\$(" + valNamePat + ")")
        stat.updated {
          valReferencePat.replaceAllIn(cmd,
            m => variables(m.group(1)))
        }
    }
  }

  val normalizeClasspath: Phase = (src, _) => {
    val pat = """\s*:\s*""".r
    pat.replaceAllIn(src, _ => ":")
  }
}

trait IssueRunnerImpl extends Phases { this: IssueRunner.type =>
  type Phase = (Tree, Context) => Tree

  @annotation.tailrec final def locateLaunchFile(dir: File): File = {
    if (dir eq null) throw new RuntimeException("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    println(s"Attempting to load $launchFile")
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }
}
