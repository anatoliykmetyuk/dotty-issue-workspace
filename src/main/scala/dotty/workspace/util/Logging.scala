package dotty.workspace.util

import fansi._

trait Logging {
  private def log(rawTag: String, attrs: Attrs, str: String) = {
    val tag = attrs(rawTag).render
    println(s"[$tag] $str")
  }

  def trace[T](name: String)(task: => T)(implicit logLevel: LogLevel): T = {
    val res = task
    debug("trace: $name = $res")
    res
  }

  def debug(str: String)(implicit logLevel: LogLevel) =
    if (logLevel.debug)
      log("debug", Back.Yellow ++ Color.LightGray, str)

  def info(str: String)(implicit logLevel: LogLevel) =
    if (logLevel.info)
      log("info", Color.Cyan, str)

  def error(str: String)(implicit logLevel: LogLevel) =
    if (logLevel.error)
      log("error", Color.Red, str)

  def fail(str: String) =
    throw new RuntimeException(str)
}
