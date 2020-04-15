package dotty.workspace
package core

import java.io.File
import scala.sys.process._
import sbt.{ Command => SBTCommandAPI, _ }

object Interpreter {
  def run(cmds: List[Command], initialState: State)(implicit ctx: Context): State = {
    var currentWorkdir = ctx.issueDir
    debug(s"Launch script:\n${cmds.mkString("\n")}")

    val failureMarker = Exec("FAILURE MARKER", None)
    var state = initialState.copy(onFailure = Some(failureMarker))
    for (cmd <- cmds) cmd match {
      case SbtCommand(cmd) =>
        info(s"> $cmd")
        state = SBTCommandAPI.process(cmd, state)
        if (state.remainingCommands.contains(failureMarker))
          fail(s"$cmd failed, see above for the detailed output")

      case ShellCommand(cmd) =>
        info(s"$currentWorkdir $$ $cmd")
        val exitCode = exec(cmd, currentWorkdir)
        if (exitCode != 0)
          fail(s"$cmd exited with status code $exitCode")

      case ChangeWorkdirCommand(wd) =>
        info(s"cd $wd")
        currentWorkdir = new File(wd)
    }
    state
  }

  private final def exec(cmd: String, workdir: File) =
    Process(List("sh", "-c", cmd), workdir).!
}
