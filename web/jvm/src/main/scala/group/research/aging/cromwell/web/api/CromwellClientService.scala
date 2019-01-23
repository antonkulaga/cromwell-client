package group.research.aging.cromwell.web.api
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce._
import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.CromwellClient
import io.circe.Json
import wvlet.log.LogSupport

trait CromwellClientService  extends Directives with FailFastCirceSupport with LogSupport  {

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case PipelinesRejections.FolderDoesNotExist(folder) =>
        complete(HttpResponse(StatusCodes.OK, entity = s"The folder ${folder} does not exist!"))
      }
      .handle { case PipelinesRejections.PipelineNotFound(pipeline) =>
        complete(HttpResponse(StatusCodes.OK, entity = s"Could not found the pipeline file at ${pipeline}"))
      }
      .result()

  /**
    * advanced withCromwell, that extracts also status and subworkfows parameters
    * @param fun
    * @return
    */
  def withCromwell(fun: (CromwellClient, client.WorkflowStatus, Boolean) => Json): Route = parameters("host".?, "status".?, "subworkflows".as[Boolean].?) {
    (hostOpt, statusOpt, incOpt) =>
      val c = hostOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
      val status = statusOpt.getOrElse(client.WorkflowStatus.AnyStatus.entryName)
      val st: client.WorkflowStatus = client.WorkflowStatus.lowerCaseNamesToValuesMap.getOrElse(status, client.WorkflowStatus.AnyStatus)
      complete(fun(c, st, incOpt.getOrElse(true)))
  }

  /**
    * Creates cromwell on the go (from host URL) and applies fun on it to return a result
    * @param fun
    * @return
    */
  def withCromwell(fun: CromwellClient => Json): Route = parameters("host".?) {
    hostOpt =>
      val c = hostOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
      complete(fun(c))
  }

  def routes: Route


}
