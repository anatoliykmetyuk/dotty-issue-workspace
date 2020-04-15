package dotty.workspace
package core

import java.io.File

import dotty.workspace.phases._

case class Context(args: List[String], issueDir: File)

object Compiler {
  val phases = List(
    MakeStatements,        // Transform a script string into a sequence of statements
    SubstituteVars,        // Traverse statements, for each, substitute $var with the value of that variable
    NormalizeSbtClasspath, // For each generated SBT command, remove whitespaces around classpath delimiters
  )

  def compile(src: String)(implicit ctx: Context): List[Command] = {
    val tree: Tree = RawScript(src)
    val launchCmds: Tree = phases.foldLeft(tree) {
      (t, phase) => phase(t) }

    def failWithBadTree(x: Tree) =
      throw new RuntimeException(
          s"After compiling the issue script, got an uninterpretable instruction: $x")

    launchCmds match {
      case Statements(stats) => stats.collect {
        case x: Command => x
        case x => failWithBadTree(x)
      }
      case x => failWithBadTree(x)
    }
  }
}
