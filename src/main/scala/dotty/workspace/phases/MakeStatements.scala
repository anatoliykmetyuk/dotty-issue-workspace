package dotty.workspace
package phases

import dotty.workspace.core._

object MakeStatements extends Phase {

  private def makeFromRawScript(src: String)(implicit ctx: Context): Tree = {
    val lines = src.split("\n")
    val stats = collection.mutable.ListBuffer.empty[Statement]
    var currentStat: Statement = null

    def removeComments(line: String): String =
      line.takeWhile(_ != '#')

    def finalizeMultilineStatement(): Unit = {
      stats += currentStat
      currentStat = null
    }

    def makeVal(line: String): ValDef = trace("makeVal") {
      val pat = s"val\\s+($valNamePat)\\s*=\\s*(.*)".r
      line match {
        case pat(name, value) => ValDef(name, value)
      }
    }

    def makeShellScript(line: String): ShellCommand = trace("makeShellScript") {
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

          currentStat =  trace("appendToCurrentStatement") {
            currentStat.map(_ + sep + str)
          }
      }
    }

    def makeSbtCommand(line: String): SbtCommand = trace("makeSbtCommand") { SbtCommand(line) }

    def makeCd(line: String): ChangeWorkdirCommand = trace("makeCd") {
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

  def apply(tree: Tree)(implicit ctx: Context): Tree =
    tree match {
      case RawScript(src) => makeFromRawScript(src)
    }
}
