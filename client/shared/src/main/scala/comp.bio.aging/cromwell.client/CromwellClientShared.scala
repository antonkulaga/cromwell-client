package comp.bio.aging.cromwell.client

import cats.MonadCombine
import fr.hmil.roshttp.{AnyBody, HttpRequest}
import fr.hmil.roshttp.body.JSONBody.JSONObject

import scala.concurrent.ExecutionContext
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import fr.hmil.roshttp.response.SimpleHttpResponse

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody._
import fr.hmil.roshttp.body._
import io.circe.Decoder.Result
import io.circe.generic.JsonCodec
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe._
import io.circe.generic.semiauto._
import cats.implicits._

trait CromwellClientShared {

  def base: String
  def version: String
  
  private implicit def tryToFuture[T](t: Try[T]): Future[T] = {
    t match{
      case Success(s) => Future.successful(s)
      case Failure(ex) => Future.failed(ex)
    }
  }


  private implicit def eitherErrorToFuture[T, TError<: Error](t: Either[TError, T]): Future[T] = {
    t match{
      case Left(e:Error) => Future.failed(e)
      case Right(s) => Future.successful(s)
    }
  }

  def waitFor[T](fut: Future[T])(implicit atMost: FiniteDuration = 10 seconds): T = {
    Await.result(fut, atMost)
  }

  def getRequest(subpath: String): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + subpath)
    request.send()
  }

  def get[T](subpath: String)(implicit decoder: Decoder[T]): Future[T] = {
    getRequest(subpath).flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def postRequest(subpath: String)(multipart: MultiPartBody): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + subpath)
    request.post(multipart)
  }

  def post[T](subpath: String)(multipart: MultiPartBody)(implicit decoder: Decoder[T]): Future[T] = {
    postRequest(subpath)(multipart).flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def getStats: Future[Stats] = get[Stats](s"/engine/${version}/stats")

  def getVersion: Future[Version] = get[Version](s"/engine/${version}/version")

  def postWorkflow(fileContent: String,
                   workflowInputs: Option[JSONObject] = None,
                   workflowOptions: Option[JSONObject] = None,
                   wdlDependencies: Option[JSONObject] = None
                  ): Future[Status] = {
    val parts = Map[String, BodyPart]("wdlSource" -> PlainTextBody(fileContent)) ++
      workflowInputs.fold(Map.empty[String, BodyPart])(part  => Map("workflowInputs" -> part))
      workflowOptions.fold(Map.empty[String, BodyPart])(part => Map("workflowOptions" -> part))
      workflowOptions.fold(Map.empty[String, BodyPart])(part => Map("wdlDependencies" -> part))
    post[Status](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def postWorkflowStrings(fileContent: String,
                   workflowInputs: String,
                   workflowOptions: String = "",
                   wdlDependencies: String = ""
                  ): Future[Status] = {
    val parts = Map[String, BodyPart](
      "wdlSource" -> PlainTextBody(fileContent),
      "workflowInputs" -> AnyBody(workflowInputs)
    )
    val partsExtended = (workflowOptions, wdlDependencies) match {
      case ("", "") => parts
      case (opts, "") =>
        parts ++ Map[String,BodyPart]("workflowOptions" -> AnyBody(opts))
      case ("", deps) =>
        parts ++ Map[String,BodyPart]("wdlDependencies" -> AnyBody(deps))
      case (opts, deps) =>
        parts ++ Map[String,BodyPart](
          "workflowOptions" -> AnyBody.apply(workflowOptions),
          "wdlDependencies" -> AnyBody.apply(wdlDependencies)
        )
    }
    post[Status](s"/workflows/${version}")(new MultiPartBody(partsExtended ))
  }

  def getOutputsRequest(id: String): Future[SimpleHttpResponse] = getRequest(s"/workflows/${version}/${id}/outputs")

  def getOutputs(id: String): Future[Outputs] = get[Outputs](s"/workflows/${version}/${id}/outputs")

  protected def queryString(status: WorkflowStatus = WorkflowStatus.Undefined) = status match {
    case WorkflowStatus.Undefined => s"/workflows/${version}/query"
    case status: WorkflowStatus =>  s"/workflows/${version}/query?status=${status.entryName}"
  }

  def getQueryRequest(status: WorkflowStatus = WorkflowStatus.Undefined): Future[SimpleHttpResponse] =
    getRequest(queryString(status))

  def getQuery(status: WorkflowStatus = WorkflowStatus.Undefined) = {
    val url = queryString(status)
    get[QueryResults](url)
  }

  def mapQuery[T](status: WorkflowStatus = WorkflowStatus.Undefined)(fun: QueryResult => Future[T]): Future[List[T]] = {
    val url = queryString(status)
    val results = get[QueryResults](url)
    results.flatMap(r=>Future.sequence(r.results.map(fun)))
  }

  def getAllOutputs(status: WorkflowStatus = WorkflowStatus.Undefined): Future[List[Outputs]] =
    mapQuery(status)(r=>this.getOutputs(r.id))

  //def getAllSucceeded: Future[QueryResults] = getQuery(WorkflowStatus.Succeeded)

  def getLogsRequest(id: String): Future[SimpleHttpResponse] = getRequest(s"/workflows/${version}/${id}/logs")

  def getLogs(id: String): Future[Logs] = get[Logs](s"/workflows/${version}/${id}/logs")

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.Undefined): Future[List[Logs]] = mapQuery(status)(r=>getLogs(r.id))

  def getBackends: Future[Backends] =  get[Backends](s"/workflows/${version}/backends")

  def getMetadataRequest(id: String): Future[SimpleHttpResponse] = getRequest(s"/workflows/${version}/${id}/metadata")

  def getMetadata(id: String): Future[Metadata] = get[Metadata](s"/workflows/${version}/${id}/metadata")

}

import enumeratum._

sealed trait WorkflowStatus extends EnumEntry

object WorkflowStatus extends Enum[WorkflowStatus] {

  /*
   `findValues` is a protected method that invokes a macro to find all `Greeting` object declarations inside an `Enum`

   You use it to implement the `val values` member
  */
  val values = findValues

  case object Submitted extends WorkflowStatus

  case object Running extends WorkflowStatus

  case object Aborting extends WorkflowStatus

  case object Failed extends WorkflowStatus

  case object Succeeded extends WorkflowStatus

  case object Aborted extends WorkflowStatus

  case object Undefined extends WorkflowStatus
}
