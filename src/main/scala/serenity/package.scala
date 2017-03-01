import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

import org.atnos.eff._

import io.circe.Json

package object serenity {
  type Stack = Fx.fx2[Eval, Either[String, ?]]

  type Effects[A] = Eff[Stack, A] // EitherT[Eval, String, A]  // TODO: name

  def handle(e: Exception): Either[String, Nothing] = {
    e.printStackTrace  // TODO: proper logging
    Left(e.getMessage)
  }

  def point[M[_], A](a: A)(implicit M: Monad[M]): M[A] = M.pure(a)  // TODO: Similar lifts: S ~> G if S :<: G - introduce types a la carte.  
}
