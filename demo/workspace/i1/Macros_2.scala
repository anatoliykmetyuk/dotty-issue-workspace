import scala.quoted.{ QuoteContext, Expr }

def h(m: Expr[M])(given QuoteContext): Expr[Any] = g(m)
