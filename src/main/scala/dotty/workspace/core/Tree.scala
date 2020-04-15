package dotty.workspace
package core

sealed trait Tree
case class RawScript(src: String) extends Tree
case class Statements(stats: List[Statement]) extends Tree

sealed trait Statement extends Tree {
  def updated(newVal: String): Statement = map(_ => newVal)

  def map(f: String => String): Statement = this match {
    case ValDef(name, oldVal) => ValDef(name, f(oldVal))
    case SbtCommand(oldVal) => SbtCommand(f(oldVal))
    case ShellCommand(oldVal) => ShellCommand(f(oldVal))
    case ChangeWorkdirCommand(oldVal) => ChangeWorkdirCommand(f(oldVal))
  }
}
case class ValDef(name: String, value: String) extends Statement

sealed trait Command extends Statement
case class SbtCommand(cmd: String) extends Command
case class ShellCommand(cmd: String) extends Command
case class ChangeWorkdirCommand(workdir: String) extends Command
