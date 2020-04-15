package dotty.workspace.util

trait Logging {
  val doDebug = true

  def trace[T](name: String)(task: => T): T = if (doDebug) {
    val res = task
    println(s"TRACE $name = $res")
    res
  } else task

  def debug(str: String) = if (doDebug) println(str)
}
