package group.research.aging.cromwell.web

import group.research.aging.cromwell.web.Messages.{ExplainedError, Info}
import mhtml.{Rx, Var}
import org.scalajs.dom.Event

import scala.xml.Elem


class ErrorsView(val errors: Rx[List[ExplainedError]], messages: Var[Messages.Message]) {

  protected def cleanErrors(event: Event): Unit = {
    messages := Messages.Errors(Nil)
  }

  val component: Rx[List[Elem]] = errors.map(ee=> ee.map(e=>
    <div class="ui negative message">
      <i class="close icon" onclick={ cleanErrors _ }></i>
      <div class="header">
        {e.title}
      </div>
      <p>{e.message}</p>
    </div>)
  )


}


class InfoView(val infos: Rx[List[Info]], messages: Var[Messages.Message]) {

  protected def cleanInfo(event: Event): Unit = {
    messages := Messages.Errors(Nil)
  }
  val component: Rx[List[Elem]] = infos.map(ii => ii.map(i=>
    <div class="ui message">
      <i class="close icon" onclick={ cleanInfo _ }></i>
      <div class="header">
        {i.title}
      </div>
      <p>{i.message}</p>
    </div>
    )
  )

}
