package group.research.aging.cromwell.client

import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body._
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.{AnyBody, HttpRequest}
import hammock.free.InterpTrans
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


  import cats._
  import cats.implicits._
  import cats.effect.IO
  import io.circe._
  import io.circe.generic.auto._
  import hammock._
  import hammock.circe.implicits._

  implicit protected def getInterpreter: InterpTrans[IO]

  def getIO(subpath: String, headers: Map[String, String]): IO[HttpResponse] =
    Hammock.request(Method.GET, Uri.unsafeParse(base + subpath), headers).exec[IO]

  def getAPI(subpath: String, headers: Map[String, String] = Map.empty): IO[HttpResponse] = getIO(api + subpath, headers)

  def getStats = getIO(s"/engine/${version}/stats", Map.empty).as[Stats]

  def getVersion = getIO(s"/engine/${version}/version", Map.empty).as[Version]

  def getEngineStatus = getIO(s"/engine/${version}/status", Map.empty)//.as[EngineStatus]


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
  def abort(id: String): IO[group.research.aging.cromwell.client.StatusInfo] =  getAPI(s"/workflows/${version}/${id}/abort").as[group.research.aging.cromwell.client.StatusInfo]

  def getOutputs(id: String): IO[Outputs] = getAPI(s"/workflows/${version}/${id}/outputs").as[Outputs]

  protected def queryString(status: WorkflowStatus = WorkflowStatus.AnyStatus): String = status match {
    case WorkflowStatus.AnyStatus => s"/workflows/${version}/query"
    case status: WorkflowStatus =>   s"/workflows/${version}/query?status=${status.entryName}"
  }

  def getQuery(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[QueryResults] = {
    val url = queryString(status)
    getAPI(url).as[QueryResults]
  }

  def getAllOutputs(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[List[Outputs]] =
    getQuery(status).flatMap(q=>
      q.results.map(r=>this.getOutputs(r.id)).sequence
    )


  def getLogs(id: String) = getAPI(s"/workflows/${version}/${id}/logs").as[Logs]

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.AnyStatus) = getQuery(status).flatMap(q=>
    q.results.map(r=>this.getLogs(r.id)).sequence
  )

  def getBackends = getAPI(s"/workflows/${version}/backends").as[Backends]

  def getMetadata(id: String, v: String = "v2", expandSubWorkflows: Boolean = true)= getAPI(s"/workflows/${v}/${id}/metadata?expandSubWorkflows=${expandSubWorkflows}").as[Metadata]

  def getAllMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus) = getQuery(status).flatMap(q=>
    q.results.map(r=>this.getMetadata(r.id)).sequence
  )

  /*
  def postIO[A: Codec[A]](subpath: String, headers: Map[String, String], value: A): IO[HttpResponse] =
    Hammock.request[A](Method.POST, Uri.unsafeParse(base + subpath), headers, Some(value)).exec[IO]

  def postAPI[A: Codec[A]](subpath: String, headers: Map[String, String], value: A): IO[HttpResponse] = postIO(api + subpath, headers, value)
*/
    /*
  def postWorkflow2(fileContent: String,
                   workflowInputs: Option[String] = None,
                   workflowOptions: Option[String] = None
                  ) = {
    val params = ("workflowSource" -> fileContent) ::
      workflowInputs.fold(List.empty[(String, String)])(part => List("workflowInputs" -> part)) ++
        workflowOptions.fold(List.empty[(String, String)])(part => List("workflowOptions" -> part))

    //postAPI(s"/workflows/${version}")(new MultiPartBody(parts))
    postAPI(s"/workflows/${version}", Map.empty, params)
  }
  */


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
                  ): Future[group.research.aging.cromwell.client.StatusInfo] = {
    val params: List[(String, BodyPart)] =
      ("workflowSource" -> PlainTextBody(fileContent)) ::
        workflowInputs.fold(List.empty[(String, BodyPart)])(part  => List("workflowInputs" -> part)) ++
        workflowOptions.fold(List.empty[(String, BodyPart)])(part  => List("workflowOptions" -> part)) ++
        workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val parts = Map[String, BodyPart](params:_*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def postWorkflowStrings(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String,
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                  ): Future[group.research.aging.cromwell.client.StatusInfo] = {
    val inputs: List[(String, BodyPart)] = if(workflowInputs == "") Nil else
      List(("workflowInputs" , AnyBody(workflowInputs)))
    val options: List[(String, BodyPart)] = if(workflowOptions == "") Nil else
      List(("workflowOptions" , AnyBody(workflowOptions)))
    val deps: List[(String, BodyPart)] =
      workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val params = ("workflowSource" , PlainTextBody(fileContent)) :: inputs ++ options ++ deps
    val parts = Map[String, BodyPart](params:_*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }


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

  case object AnyStatus extends WorkflowStatus
}
