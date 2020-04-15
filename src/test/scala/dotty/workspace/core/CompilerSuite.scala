package dotty.workspace.core

import java.util.Properties

import utest._

object CompilerSuite extends TestSuite {
  implicit val ctx = {
    val props = new Properties()
    props.setProperty("workspace_path", "dummy/workspace")
    Context(Nil, "dummy", props)
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
    test("Compiler tests"){
      test - check(
        """
          |foo
        """,
        """
          |> foo
        """)
    }
  }
}