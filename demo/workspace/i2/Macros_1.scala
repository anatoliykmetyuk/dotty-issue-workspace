import scala.quoted.{ QuoteContext, Expr }

trait Foo

inline def g(em: Expr[Foo])(given QuoteContext) = '{$em}
