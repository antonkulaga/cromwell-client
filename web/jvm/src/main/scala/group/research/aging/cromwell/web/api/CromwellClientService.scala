package group.research.aging.cromwell.web.api
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Route, _}
import akka.util.Timeout
import better.files.File
import de.heikoseeberger.akkahttpcirce._
import group.research.aging.cromwell.client.{CromwellClient, WorkflowStatus}
import group.research.aging.cromwell.web.util.PipelinesExtractor
import wvlet.log.LogSupport

import scala.concurrent.Future

trait BasicService extends Directives with FailFastCirceSupport with LogSupport {

  def routes: Route

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case PipelinesRejections.FolderDoesNotExist(folder) =>
        complete(HttpResponse(StatusCodes.OK, entity = s"The folder ${folder} does not exist!"))
      }
      .handle { case PipelinesRejections.PipelineNotFound(pipeline) =>
        complete(HttpResponse(StatusCodes.OK, entity = s"Could not found the pipeline file at ${pipeline}"))
      }
      .result()
}
/**
  * Basic trait for services that have to deal with CromwellServer
  */
trait CromwellClientService extends BasicService with PipelinesExtractor  {

  def runner: ActorRef
  implicit def timeout: Timeout

/*
  def withServerMessage(fun: (RestAPI, client.WorkflowStatus, Boolean) => Json): Route = parameters("server".?, "status".?, "subworkflows".as[Boolean].?) {
    (serverOpt, statusOpt, incOpt) =>
      val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
      val status = statusOpt.getOrElse(client.WorkflowStatus.AnyStatus.entryName)
      val st: client.WorkflowStatus = client.WorkflowStatus.lowerCaseNamesToValuesMap.getOrElse(status, client.WorkflowStatus.AnyStatus)
      complete(fun(c, st, incOpt.getOrElse(true)))
  }
*/

  def withServerExtended[T](operationName: String)(fun: (String, WorkflowStatus, Boolean) => Future[T])(implicit m: ToResponseMarshaller[T]): Route =
    parameters("server".?, "status".?, "subworkflows".as[Boolean].?) {
    (serverOpt, statusOpt, incOpt) =>
      val server: String = serverOpt.getOrElse(CromwellClient.defaultURL)
      val status: WorkflowStatus = statusOpt.map(s=>WorkflowStatus.lowerCaseNamesToValuesMap(s.toLowerCase)).getOrElse(WorkflowStatus.AnyStatus)
      val subs = incOpt.getOrElse(true)
      completeOrRecoverWith(fun(server, status, subs)) { extraction =>
        debug(s"running operation $operationName with server ${server} failed with ${extraction}")
        failWith(extraction) // not executed.
      }
      complete(fun(server, status, subs))
  }

  def withServer[T](operationName: String)(fun: String => Future[T])(implicit m: ToResponseMarshaller[T]): Route =  parameters("server".?) {
    serverOpt =>
      val s: String = serverOpt.getOrElse(CromwellClient.defaultURL)
      val server = if(s.endsWith("/")) s.dropRight(1) else s
      completeOrRecoverWith(fun(server)) { extraction =>
        debug(s"running operation $operationName with server ${server} failed with ${extraction}")
        failWith(extraction) // not executed.
      }
  }

  /*
  def withCromwell(fun: CromwellClient => Json): Route = parameters("server".?) {
    serverOpt =>
      val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
      complete(fun(c))
  }
  def withCromwell(fun: (CromwellClient, client.WorkflowStatus, Boolean) => Json): Route = parameters("server".?, "status".?, "subworkflows".as[Boolean].?) {
    (serverOpt, statusOpt, incOpt) =>
      val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
      val status = statusOpt.getOrElse(client.WorkflowStatus.AnyStatus.entryName)
      val st: client.WorkflowStatus = client.WorkflowStatus.lowerCaseNamesToValuesMap.getOrElse(status, client.WorkflowStatus.AnyStatus)
      complete(fun(c, st, incOpt.getOrElse(true)))
  }
  */

}

