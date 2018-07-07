package group.research.aging.cromwell.web

import group.research.aging.cromwell.web.Messages.ExplainedError
import mhtml.{Rx, Var}
import org.scalajs.dom.Event

import scala.xml.Elem

class ErrorsView(val errors: Rx[List[ExplainedError]], messages: Var[Messages.Message]) {

  protected def updateClick(event: Event): Unit = {
    messages := Messages.Errors(Nil)
  }

  val component: Rx[List[Elem]] =  errors.map(ee=> ee.map(e=>
    <div class="ui negative message">
      <i class="close icon" onclick={ updateClick _ }></i>
      <div class="header">
        {e.message}
      </div>
      <p>{e.errorMessage}</p>
    </div>
    )
  )

}
