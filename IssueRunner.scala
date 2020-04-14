import sbt.{ Command => SBTCommandAPI, _ }, sbt.internal.util.AttributeMap
import Keys._
import complete.DefaultParsers._

import java.io.File

import scala.sys.process._
import scala.io.Source


sealed trait Tree
case class RawScript(src: String) extends Tree
case class Statements(stats: List[Statement]) extends Tree

sealed trait Statement extends Tree {
  def updated(newVal: String): Statement = map(_ => newVal)

  def map(f: String => String): Statement = this match {
    case ValDef(name, oldVal) => ValDef(name, f(oldVal))
    case SbtCommand(oldVal) => SbtCommand(f(oldVal))
    case ShellCommand(oldVal) => ShellCommand(f(oldVal))
    case ChangeWorkdirCommand(oldVal) => ShellCommand(f(oldVal))
  }
}
case class ValDef(name: String, value: String) extends Statement

sealed trait Command extends Statement
case class SbtCommand(cmd: String) extends Command
case class ShellCommand(cmd: String) extends Command
case class ChangeWorkdirCommand(workdir: String) extends Command

case class Context(args: List[String], issueDir: File)

object IssueRunner extends AutoPlugin with IssueRunnerPhases {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  val issuesWorkspaceAttr = AttributeKey[File]("issueWorkspace")

  def issuesWorkspace = SBTCommandAPI.args("issuesWorkspace", "<workdir>") { case (state, dirName :: Nil) =>
    val newState = state.copy(attributes = state.attributes.put(issuesWorkspaceAttr, new File(dirName)))
    println(s"Issues Workspace is set to ${newState.attributes(issuesWorkspaceAttr)}")
    newState
  }

  def issue = SBTCommandAPI.args("issue", "<dirName>, <args>") { case (initialState, dirName :: args) =>
    val issuesWorkspace = initialState.attributes(issuesWorkspaceAttr)
    val issueDir  = new File(issuesWorkspace, dirName)
    val launchSrc = locateLaunchFile(issueDir)

    val phases = List(
      makeStatements,        // Transform a script string into a sequence of statements
      substituteVars,        // Traverse statements, for each, substitute $var with the value of that variable
      normalizeSbtClasspath, // For each generated SBT command, remove whitespaces around classpath delimiters
    )

    val ctx = Context(args, issueDir)
    val src: Tree = RawScript(Source.fromFile(launchSrc).mkString)
    val launchCmds: Tree = phases.foldLeft(src) {
      (src, phase) => phase(src, ctx) }

    var currentWorkdir = ctx.issueDir
    launchCmds match {
      case Statements(cmds) =>
        var state = initialState
        for (cmd <- cmds) cmd match {
          case SbtCommand(cmd) =>
            println(s"Executing SBT command:\n$cmd")
            state = SBTCommandAPI.process(cmd, initialState)

          case ShellCommand(cmd) =>
            println(s"Executing shell command at $currentWorkdir:\n$cmd")
            exec(cmd, currentWorkdir)

          case ChangeWorkdirCommand(wd) =>
            currentWorkdir = new File(wd)
        }
        state

      case x => throw new RuntimeException(
        s"Expected a list of commands after issue script processing, got: $x")
    }
  }

  @annotation.tailrec
  private final def locateLaunchFile(dir: File): File = {
    if (dir eq null) throw new RuntimeException("Can't locate the launch file")

    val launchFile = new File(dir, "launch.iss")
    println(s"Attempting to load $launchFile")
    if (launchFile.exists) launchFile
    else locateLaunchFile(dir.getParentFile)
  }

  private final def exec(cmd: String, workdir: File) =
    Process(cmd.dropWhile(_.isWhitespace), workdir).!

  override lazy val projectSettings = Seq(commands ++= Seq(issue, issuesWorkspace))
}

trait IssueRunnerPhases { this: IssueRunner.type =>
  type Phase = (Tree, Context) => Tree

  val valNamePat = """[\w\-_\d]+"""

  val makeStatements: Phase = { case (RawScript(src), _) =>
    val lines = src.split("\n")
    val stats = collection.mutable.ListBuffer.empty[Statement]
    var currentStat: Statement = null

    def removeComments(line: String): String =
      line.takeWhile(_ != '#')

    def finalizeMultilineStatement(): Unit = {
      stats += currentStat
      currentStat = null
    }

    def makeVal(line: String): ValDef = {
      val pat = s"val\\s+($valNamePat)\\s*=\\s*(.*)".r
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
        case pat(str) =>
          val useWhitespaceSeparator =
            !currentStat.isInstanceOf[ChangeWorkdirCommand]
          val sep = if (useWhitespaceSeparator) " " else ""

          currentStat = currentStat.map(_ + sep + str)
      }
    }

    def makeSbtCommand(line: String): SbtCommand = SbtCommand(line)

    def makeCd(line: String): ChangeWorkdirCommand = {
      val pat = """cd\s+(.+)""".r
      line match {
        case pat(str) => ChangeWorkdirCommand(str)
      }
    }

    for {
      rawLine <- lines
      line = removeComments(rawLine)
      if line.nonEmpty && !line.forall(_.isWhitespace)
    } {
      if (line.startsWith(" ")) appendToCurrentStatement(line)
      else {
        if (currentStat != null) finalizeMultilineStatement()
        currentStat =
          if (line.startsWith("val")) makeVal(line)
          else if (line.startsWith("$")) makeShellScript(line)
          else if (line.startsWith("cd")) makeCd(line)
          else makeSbtCommand(line)
      }
    }

    if (currentStat != null) finalizeMultilineStatement()
    Statements(stats.toList)
  }

  val substituteVars: Phase = { case (Statements(stats), ctx) =>
    val variables = collection.mutable.Map[String, String]("here" -> ctx.issueDir.getPath)
    for { (arg, id) <- ctx.args.zipWithIndex }
      variables.updated((id + 1).toString, arg)

    val newStats =
      stats.flatMap {
        case ValDef(name, value) =>
          variables.update(name, value)
          Nil

        case stat: Command =>
          val valReferencePat = ("\\$(" + valNamePat + ")").r
          stat.map { cmd =>
            valReferencePat.replaceAllIn(cmd,
              m => variables(m.group(1)))
          } :: Nil
      }

    Statements(newStats)
  }

  val normalizeSbtClasspath: Phase = { case (Statements(stats), _) =>
    val newStats =
      stats.map {
        case SbtCommand(cmd) =>
          val pat = """\s*:\s*""".r
          SbtCommand(pat.replaceAllIn(cmd, _ => ":"))
        case x => x
      }
    Statements(newStats)
  }
}
