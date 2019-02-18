package group.research.aging.cromwell.web
import mhtml.{Rx, Var}
import wvlet.log.LogSupport

import scala.scalajs.js

trait Base extends LogSupport
{

  /**
    * Reducer type, all State updates happen inside the reducers that are partial functions receiving State and Action, returning new value of the State
    */
  type Reducer = PartialFunction[(State, Action), State]

  def enabledIf(str: String, condition: Rx[Boolean]): Rx[String] =
    condition.map(u=>
      if (u) {
        str
      } else s"$str disabled"
    )

  def updateClick[T](value: Var[T], updateValue: T): js.Dynamic => Unit = { _ =>
    value := updateValue
  }

  def un(str: String) = scala.xml.Unparsed(str)

}
