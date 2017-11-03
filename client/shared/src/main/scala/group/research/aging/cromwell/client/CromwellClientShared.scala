package group.research.aging.cromwell.client

import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body._
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.{AnyBody, HttpRequest}
import io.circe._
import io.circe.parser._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait CromwellClientShared {

  def base: String
  def version: String

  lazy val api = "/api"

  def makeWorkflowOptions(output: String, log: String="", call_log: String = ""): String =
    s"""
      |{
      | "write_to_cache": true,
      | "read_from_cache": true,
      | "final_workflow_outputs_dir": "${output}",
      | ${if(log!="") s""" "final_workflow_log_dir": "${log}", """ else ""}
      | ${if (call_log != "") s""" "final_call_logs_dir": "${call_log}", """ else ""}
      |}
    """.stripMargin


  private implicit def tryToFuture[T](t: Try[T]): Future[T] = {
    t match{
      case Success(s) => Future.successful(s)
      case Failure(ex) => Future.failed(ex)
    }
  }


  private implicit def eitherErrorToFuture[T, TError<: Error](t: Either[TError, T]): Future[T] = {
    t match{
      case Left(e: Error) => Future.failed(e)
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

  def getRequestAPI(subpath: String): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + api + subpath)
    request.send()
  }

  protected def get[T](request: Future[SimpleHttpResponse])(implicit decoder: Decoder[T]): Future[T] = {
    request.flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def get[T](subpath: String)(implicit decoder: Decoder[T]): Future[T] = {
    get[T](getRequest(subpath))(decoder)
  }

  def getAPI[T](subpath: String)(implicit decoder: Decoder[T]): Future[T] = {
    get[T](api + subpath)(decoder)
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

  def postAPI[T](subpath: String)(multipart: MultiPartBody)(implicit decoder: Decoder[T]): Future[T] =
    post[T](api + subpath)(multipart)(decoder)

  def getStats: Future[Stats] = get[Stats](s"/engine/${version}/stats")

  def getVersion: Future[Version] = get[Version](s"/engine/${version}/version")

  /**
    * 400
    * Malformed Workflow ID
    * 403
    * Workflow in terminal status
    * 404
    * Workflow ID Not Found
    * 500
    * Internal Error
    */
  def abort(id: String) =  getAPI[Status](s"/workflows/${version}/${id}/abort")

  /**
    * 400
    * Malformed Input
    * 500
    * Internal Error
    * @param fileContent
    * @param workflowInputs
    * @param workflowOptions
    * @param workflowDependencies
    * @return
    */
  def postWorkflow(fileContent: String,
                   workflowInputs: Option[JSONObject] = None,
                   workflowOptions: Option[JSONObject] = None,
                   workflowDependencies: Option[java.nio.ByteBuffer] = None
                  ): Future[Status] = {
    val params: List[(String, BodyPart)] =
      ("workflowSource" -> PlainTextBody(fileContent)) ::
        workflowInputs.fold(List.empty[(String, BodyPart)])(part  => List("workflowInputs" -> part)) ++
        workflowOptions.fold(List.empty[(String, BodyPart)])(part  => List("workflowOptions" -> part)) ++
        workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val parts = Map[String, BodyPart](params:_*)
    postAPI[Status](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def postWorkflowStrings(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String,
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                  ): Future[Status] = {
    val inputs: List[(String, BodyPart)] = if(workflowInputs == "") Nil else
      List(("workflowInputs" , AnyBody(workflowInputs)))
    val options: List[(String, BodyPart)] = if(workflowOptions == "") Nil else
      List(("workflowOptions" , AnyBody(workflowOptions)))
    val deps: List[(String, BodyPart)] =
      workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val params = ("workflowSource" , PlainTextBody(fileContent)) :: inputs ++ options ++ deps
    val parts = Map[String, BodyPart](params:_*)
    postAPI[Status](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def getOutputsRequest(id: String): Future[SimpleHttpResponse] = getRequest(api + s"/workflows/${version}/${id}/outputs")

  def getOutputs(id: String): Future[Outputs] = getAPI[Outputs](s"/workflows/${version}/${id}/outputs")

  protected def queryString(status: WorkflowStatus = WorkflowStatus.Undefined) = status match {
    case WorkflowStatus.Undefined => api + s"/workflows/${version}/query"
    case status: WorkflowStatus =>  api + s"/workflows/${version}/query?status=${status.entryName}"
  }

  def getQueryRequest(status: WorkflowStatus = WorkflowStatus.Undefined): Future[SimpleHttpResponse] =
    getRequest(api + queryString(status))

  def getQuery(status: WorkflowStatus = WorkflowStatus.Undefined) = {
    val url = queryString(status)
    get[QueryResults](url)
  }

  def mapQuery[T](status: WorkflowStatus = WorkflowStatus.Undefined)(fun: QueryResult => Future[T]): Future[List[T]] = {
    val url = queryString(status)
    val results: Future[QueryResults] = get[QueryResults](url)
    results.flatMap(r=>Future.sequence(r.results.map(fun)))
  }

  def getAllOutputs(status: WorkflowStatus = WorkflowStatus.Undefined): Future[List[Outputs]] =
      mapQuery(status)(r=>this.getOutputs(r.id))

  //def getAllSucceeded: Future[QueryResults] = getQuery(WorkflowStatus.Succeeded)

  def getLogsRequest(id: String): Future[SimpleHttpResponse] = getRequest(api + s"/workflows/${version}/${id}/logs")

  def getLogs(id: String): Future[Logs] = get[Logs](getLogsRequest(id))

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.Undefined): Future[List[Logs]] = mapQuery(status)(r=>getLogs(r.id))

  def getBackends: Future[Backends] =  getAPI[Backends](s"/workflows/${version}/backends")

  def getMetadataRequest(id: String): Future[SimpleHttpResponse] = getRequest(api + s"/workflows/${version}/${id}/metadata")

  def getMetadata(id: String): Future[Metadata] = get[Metadata](getMetadataRequest(id))

  def getAllMetadata(status: WorkflowStatus = WorkflowStatus.Undefined) =  mapQuery(status)(r=>getMetadata(r.id))

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
