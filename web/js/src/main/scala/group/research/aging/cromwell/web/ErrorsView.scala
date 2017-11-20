package group.research.aging.cromwell.web

import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.Metadata
import group.research.aging.cromwell.web.Messages.ExplainedError
import mhtml.Var
import org.scalajs.dom.Event

class ErrorsView(dispatcher: Dispatcher) {

  val errors = Var(List.empty[ExplainedError])

  protected def updateClick(event: Event): Unit = {
    dispatcher.dispatch(Messages.Errors(Nil))
  }

  val component =  errors.map(ee=> ee.map(e=>
    <div class="ui negative message">
      <i class="close icon" onclick={ updateClick _ }></i>
      <div class="header">
        {e.message}
      </div>
      <p>{e.errorMessage}</p>
    </div>
    )
  )

  def onUpdate( reader: ModelRO[List[ExplainedError]]): Unit = {
    errors := reader.value
  }

}
