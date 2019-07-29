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

}
