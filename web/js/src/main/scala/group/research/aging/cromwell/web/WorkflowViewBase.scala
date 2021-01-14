package group.research.aging.cromwell.web
//import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{LogCall, Metadata, WorkflowFailure}
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.Event
import scalajs.js
import scala.xml.Elem

import java.time.format.DateTimeFormatter
//import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

@js.native
@js.annotation.JSGlobalScope
object Global extends js.Object {
  def renderjson(obj: js.Any): dom.html.Element = js.native
}

trait WorkflowViewBase {

  def un(str: String): String = str.replace("\\\"","")
  protected def messageClass(r: Metadata, tp: String = "positive") = if(r.parentWorkflowId.isDefined) s"ui ${tp} tiny message" else s"ui ${tp} small message"

  def clientPort = dom.window.location.port match {
    case "" => ""
    case v => ":" + v
  }

  def statusClass(str: String): String = str.toLowerCase match {
    case "succeeded" | "done" => "positive"
    case "failed" | "aborted" => "negative"
    case _ => "warning"
  }

}
