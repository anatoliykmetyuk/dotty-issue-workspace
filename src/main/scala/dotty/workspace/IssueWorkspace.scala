package dotty.workspace

import sbt.{ Command => SBTCommandAPI, _ }
import Keys._
import complete.DefaultParsers._

import java.io.{ File, FileReader }
import java.util.Properties

import scala.io.Source

import dotty.workspace.core._


object IssueWorkspace extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  def issue = SBTCommandAPI.args("issue", "<dirName>, <args>") { case (initialState, issueName :: args) =>
    val props = loadProperties(s"${sys.props("user.home")}/.sbt/1.0/plugins/dotty-workspace-path")
    implicit val ctx = Context(args, issueName, props)

    val src = Source.fromFile(ctx.launchFile).mkString
    val launchCmds = Compiler.compile(src)
    Interpreter.run(launchCmds, initialState)
  }

  private def loadProperties(path: String): Properties = {
    val reader = new FileReader(path)
    try {
      val props = new Properties()
      props.load(reader)
      props
    }
    finally reader.close()
  }

  override lazy val projectSettings = Seq(commands += issue)
}
