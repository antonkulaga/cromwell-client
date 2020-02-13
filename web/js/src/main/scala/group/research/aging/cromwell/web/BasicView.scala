package group.research.aging.cromwell.web

import mhtml.Rx

trait BasicView {

  def enabledIf(str: String, condition: Rx[Boolean]): Rx[String] =
    condition.map(u=>
      if (u) {
        str
      } else s"$str disabled"
    )

  def stringIfElse(condition: Rx[Boolean], ifYes: String, ifNot: String): Rx[String] = {
    condition.map(u=>
      if (u) ifYes else ifNot
    )
  }

  def visibleIf(condition: Rx[Boolean]) = stringIfElse(condition,"display:flex", "display:none")
  def visibleIfDefined(condition: Rx[Option[_]]) = stringIfElse(condition.map(_.isDefined),"display:flex", "display:none")
  def visibleIfHasElements(condition: Rx[List[_]]) = stringIfElse(condition.map(_.nonEmpty),"display:flex", "display:none")

  def visibleIfEmpty(condition: Rx[Option[_]]) = stringIfElse(condition.map(_.isEmpty),"display:flex", "display:none")


}
