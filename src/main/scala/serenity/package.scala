import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

package object serenity {
  type Stuff[A] = EitherT[Eval, String, A]  // TODO: name

  def handle(e: Exception): Either[String, Nothing] = {
    e.printStackTrace  // TODO: proper logging
    Left(e.getMessage)
  }

  def point[M[_], A](a: A)(implicit M: Monad[M]): M[A] = M.pure(a)  // TODO: Similar lifts: S ~> G if S :<: G - introduce types a la carte.  

  val workdirPath = "workdir"
}