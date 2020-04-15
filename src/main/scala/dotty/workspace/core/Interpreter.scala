package dotty.workspace
package core

import java.io.File
import scala.sys.process._
import sbt.{ Command => SBTCommandAPI, _ }

object Interpreter {
  def run(cmds: List[Command], initialState: State, workdir: File): State = {
    var currentWorkdir = workdir
    debug(s"Launch script:\n${cmds.mkString("\n")}")

    val failureMarker = Exec("FAILURE MARKER", None)
    var state = initialState.copy(onFailure = Some(failureMarker))
    for (cmd <- cmds) cmd match {
      case SbtCommand(cmd) =>
        debug(s"> $cmd")
        state = SBTCommandAPI.process(cmd, state)
        if (state.remainingCommands.contains(failureMarker))
          throw new RuntimeException(s"$cmd failed, see above for the detailed output")

      case ShellCommand(cmd) =>
        debug(s"$currentWorkdir $$ $cmd")
        val exitCode = exec(cmd, currentWorkdir)
        if (exitCode != 0)
          throw new RuntimeException(s"$cmd exited with status code $exitCode")

      case ChangeWorkdirCommand(wd) =>
        debug(s"cd $wd")
        currentWorkdir = new File(wd)
    }
    state
  }

  private final def exec(cmd: String, workdir: File) =
    Process(List("sh", "-c", cmd), workdir).!
}
