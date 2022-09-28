package scala.compat

import scala.collection.BuildFrom
import scala.language.higherKinds

object CompatBuildFrom {
  /**
    * Can be used in place of `implicitly` when the implicit type is a BuildFrom to assist in typechecking
    *
    * @example Future.sequence(myFutures)(implicitlyBF, ExecutionContexts.global)
    */
  def implicitlyBF[A, B, M[X] <: IterableOnce[X]](implicit
    bf: BuildFrom[M[A], B, M[B]]
  ): BuildFrom[M[A], B, M[B]] = bf
}
