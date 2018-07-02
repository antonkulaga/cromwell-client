package group.research.aging.cromwell.web

import diode.{ActionHandler, Circuit, Effect}
import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{CromwellClientLike, Metadata}
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class AppModel(
                     client: CromwellClientLike,
                     metadata: List[Metadata],
                     errors: List[Messages.ExplainedError] = Nil
                   )

object AppCircuit extends Circuit[AppModel] {
  // provides initial model to the Circuit
  override def initialModel = AppModel(CromwellClient.localhost, Nil)

  private val clientRequests = new ActionHandler(zoomTo(_.client)) {
    override protected def handle = {

      case Commands.GetMetadata(status)=>
        effectOnly(Effect(value.getAllMetadata(status).map{md=>
          Results.UpdatedMetadata(md)
        }.unsafeToFuture().recover{
          case th =>
            Messages.Errors(Messages.ExplainedError(s"getting information from the server failed ${value.base}", Option(th.getMessage).getOrElse(""))::Nil)
        })
        )

      case Commands.ChangeClient(url) =>
        if(value.base != url) {
          dom.window.localStorage.setItem(Commands.LoadLastUrl.key, url)
          updated(CromwellClient(url))
        } else noChange

      case Commands.LoadLastUrl =>
        Option(dom.window.localStorage.getItem(Commands.LoadLastUrl.key)).fold(
          noChange)(url=> updated(CromwellClient(url)))

      case Commands.Run(wdl, input, options) =>
        effectOnly(
          Effect(value.postWorkflowStrings(wdl, input, options).map(md=>Results.UpdatedStatus(md))
            .recover{
              case th =>
                Messages.Errors(Messages.ExplainedError(s"running workflow at ${value.base} failed", Option(th.getMessage).getOrElse(""))::Nil)
            }
          ) >>
            Effect(value.getAllMetadata().map(md=>Results.UpdatedMetadata(md)).unsafeToFuture().recover{
              case th =>
                Messages.Errors(Messages.ExplainedError(s"getting information from the server failed ${value.base}", Option(th.getMessage).getOrElse(""))::Nil)
            })
        )
    }
  }

  private val metadataHandler = new ActionHandler(zoomTo(_.metadata)){
    override protected def handle =
    {
      case  Results.UpdatedMetadata(data) =>
        updated(data)
    }
  }

  private val errorsHandler = new ActionHandler(zoomTo(_.errors)){
    override protected def handle =
    {
      case Messages.Errors(ers) => updated(ers)
      case Commands.GetMetadata(status) => updated(Nil)
    }
  }

  override protected def actionHandler = foldHandlers(composeHandlers(clientRequests, metadataHandler), errorsHandler)
}
