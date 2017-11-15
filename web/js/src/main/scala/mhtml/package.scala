import mhtml.Var
import cats._
import cats.implicits._

package object mhtml {

  implicit class VarExt[T](v: Var[T]) {
    def now(implicit monoid: Monoid[T]): T = v.cacheElem.getOrElse(monoid.empty)
    def getOrElse(value: T): T = v.cacheElem.getOrElse(value)
  }

}