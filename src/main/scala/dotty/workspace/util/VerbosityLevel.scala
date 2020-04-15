package dotty.workspace.util

import dotty.workspace.core._


class LogLevel(val level: Int) {
  def debug = level >= LogLevel.Debug.level
  def info = level >= LogLevel.Info.level
  def error = level >= LogLevel.Error.level
}

object LogLevel {
  implicit def fromContext(implicit ctx: Context): LogLevel = ctx.logLevel

  def apply(str: String): LogLevel = str.toLowerCase match {
    case "error" => Error
    case "info" => Info
    case "debug" => Debug
  }

  val Error = new LogLevel(0)
  val Info = new LogLevel(1)
  val Debug = new LogLevel(2)
}
