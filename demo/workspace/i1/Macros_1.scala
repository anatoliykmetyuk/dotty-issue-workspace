import scala.quoted.{ QuoteContext, Expr }

trait M {
  def f: Any
}

inline def g(em: Expr[M])(given QuoteContext) = '{$em.f}
