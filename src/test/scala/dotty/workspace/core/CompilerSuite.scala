package dotty.workspace.core

import java.util.Properties

import utest._

object CompilerSuite extends TestSuite {
  implicit val ctx = {
    val props = new Properties()
    props.setProperty("workspace_path", "dummy/workspace")
    Context("first" :: "second" :: Nil, "issue", props)
  }

  def check(scriptRaw: String, compiledRaw: String)(implicit ctx: Context): Unit = {
    def stripWhitespacePadding(str: String) =
      str.stripMargin.dropWhile(_.isWhitespace)
        .reverse.dropWhile(_.isWhitespace).reverse

    val script = stripWhitespacePadding(scriptRaw)
    val compiled = stripWhitespacePadding(compiledRaw)
    val actualCompiled = Compiler.compile(script).mkString("\n")

    assert(compiled == actualCompiled)
  }

  val tests = Tests {
    test("Syntax"){
      test("SBT command") - check(
        """
          |foo
        """,
        """
          |> foo
        """)

      test("Shell command") - check(
        """
          |$ foo
        """,
        """
          |$ foo
        """
      )

      test("cd") - check(
        """
          |cd foo
        """,
        """
          |cd foo
        """
      )

      test("Comments") - check(
        """
          |# Comment
          |cmd# Execute some command
        """,
        """
          |> cmd
        """
      )

      test("Sequential commands") - check(
        """
          |foo
          |bar
          |$ char
        """,
        """
          |> foo
          |> bar
          |$ char
        """
      )

      test("Multiline command") - check(
        """
          |foo
          |  -d stuff
          |  -a stuff
        """,
        """
          |> foo -d stuff -a stuff
        """
      )

      test("Variables") {
        test("$here") - check(
          """
            |go $here
          """,
          """
            |> go dummy/workspace/issue
          """
        )

        test("Script varaibles") - check(
          """
            |do $1 $2
          """,
          """
            |> do first second
          """
        )

        test("Custom") - check(
          """
            |val variable = foo
            |do $variable
          """,
          """
            |> do foo
          """
        )
      }
    }
  }
}