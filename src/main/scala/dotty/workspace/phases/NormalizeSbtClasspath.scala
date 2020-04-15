package dotty.workspace
package phases

import dotty.workspace.core._


object NormalizeSbtClasspath extends Phase {
  def apply(tree: Tree)(implicit ctx: Context): Tree = tree match {
    case Statements(stats) =>
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
