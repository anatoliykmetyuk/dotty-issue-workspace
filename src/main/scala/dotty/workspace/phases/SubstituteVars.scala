package dotty.workspace
package phases

import dotty.workspace.core._


object SubstituteVars extends Phase {
  private def substituteInStats(stats: List[Statement])(implicit ctx: Context): Statements = {
    val variables = collection.mutable.Map[String, String]("here" -> ctx.issueDir.getPath)
    for { (arg, id) <- ctx.args.zipWithIndex }
      variables.updated((id + 1).toString, arg)

    def substituteVars(str: String): String = {
      val valReferencePat = ("\\$(" + valNamePat + ")").r
      valReferencePat.replaceAllIn(str,
        m => variables(m.group(1)))
    }

    val newStats =
      stats.flatMap {
        case ValDef(name, value) =>
          variables.update(name, substituteVars(value))
          Nil

        case stat: Command =>
          stat.map(substituteVars) :: Nil
      }

    Statements(newStats)
  }

  def apply(tree: Tree)(implicit ctx: Context): Tree = tree match {
    case Statements(stats) => substituteInStats(stats)
  }
}
