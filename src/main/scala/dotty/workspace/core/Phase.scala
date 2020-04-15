package dotty.workspace
package core

trait Phase {
  def apply(tree: Tree)(implicit ctx: Context): Tree
}
