import sbt._, sbt.internal.util.AttributeMap
import Keys._
import complete.DefaultParsers._
import scala.sys.process._

object IssueRunner extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  val issuesWorkspaceAttr = AttributeKey[String]("issueWorkspace")

  def issuesWorkspace = Command.args("issuesWorkspace", "<workdir>") { case (state, dirName :: Nil) =>
    val newState = state.copy(attributes = state.attributes.put(issuesWorkspaceAttr, dirName))
    println(s"Issues Workspace is set to ${newState.attributes(issuesWorkspaceAttr)}")
    newState
  }

  def issue = Command.args("issue", "<dirName>") { case (state, dirName :: Nil) =>
    @annotation.tailrec def locateLaunchFile(dirRaw: String): String = {
      val dir = dirRaw.reverse.dropWhile(_ == '/').reverse
      val launchFile = s"$dir/launch.iss"
      println(s"Attempting to load $launchFile")
      if (file(launchFile).exists) launchFile
      else if (augmentString(dir).filter(_ != '/').isEmpty) throw new RuntimeException("Can't locate the launch file")
      else locateLaunchFile(dir.reverse.dropWhile(_ != '/').reverse)
    }

    val issuesWorkspace = state.attributes(issuesWorkspaceAttr)
    val issueDir  = s"$issuesWorkspace/$dirName"
    val launchSrc = locateLaunchFile(issueDir)

    val compileLaunchCmd = List(
      "awk",
      "-F", "#",
      """
      |/^[[:space:]]*#/ || /^$/ { next }
      |!/^[[:space:]]/ { printf ";%s",$1; next }
      |{ printf "%s",$1 }
      |""".stripMargin,
      launchSrc)

    val res = compileLaunchCmd.lineStream.head
      .replace("$here", issueDir)

    println(s"Executing command:\n$res")
    Command.process(res, state)
  }

  override lazy val projectSettings = Seq(commands ++= Seq(issue, issuesWorkspace))
}
