import scala.quoted.{ QuoteContext, Expr }

def h(m: Expr[Foo])(given QuoteContext): Expr[Any] = g(m)
