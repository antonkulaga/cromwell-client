package group.research.aging.cromwell.web

import diode.{ActionHandler, Circuit, Effect}
import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.Metadata
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class AppModel(
                     client: CromwellClient,
                     metadata: List[Metadata]
                   )

object AppCircuit extends Circuit[AppModel] {
  // provides initial model to the Circuit
  override def initialModel = AppModel(CromwellClient.localhost, Nil)

  val requests = new ActionHandler(zoomTo(_.client)) {
    override protected def handle = {
      case Commands.GetMetadata(status)=>
        effectOnly(Effect(value.getAllMetadata(status).map(md=>Results.UpdatedMetadata(md))))

      case Commands.ChangeClient(url) =>
        dom.window.localStorage.setItem(Commands.LoadLastUrl.key, url)
        updated(CromwellClient(url))

      case Commands.LoadLastUrl =>
        Option(dom.window.localStorage.getItem(Commands.LoadLastUrl.key)).fold(
          noChange)( url=> updated(CromwellClient(url)))
    }
  }

  val metadataHandler = new ActionHandler(zoomTo(_.metadata)){
    override protected def handle =
    {
      case  Results.UpdatedMetadata(data) => updated(data)
    }
  }

  override protected def actionHandler = composeHandlers(requests, metadataHandler)
}
