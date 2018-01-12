package group.research.aging.cromwell.client

import cats.effect.IO
import cats.implicits._

import hammock._
import hammock.marshalling._
import hammock.Decoder
import hammock.circe.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import cats.syntax.show._
import cats.free.Free

import hi.Opts
import Codec.ops._

trait CromwellClientShared extends RosHttp {

  def base: String
  def version: String

  lazy val api = "/api"

  def makeWorkflowOptions(output: String, log: String="", call_log: String = ""): String =
    s"""
      |{
      | "write_to_cache": true,
      | "read_from_cache": true,
      | "final_workflow_CallOutputs_dir": "${output}",
      | ${if(log!="") s""" "final_workflow_log_dir": "${log}", """ else ""}
      | ${if (call_log != "") s""" "final_call_logs_dir": "${call_log}", """ else ""}
      |}
    """.stripMargin



  implicit protected def getInterpreter: InterpTrans[IO]
  //implicit val interpTrans = Interpreter[IO]

  def get(subpath: String, headers: Map[String, String]): Free[HttpF, HttpResponse] = Hammock.request(Method.GET, Uri.unsafeParse(base + subpath), headers)

  //def post[T](subpath: String, headers: Map[String, String], body: T): Free[HttpF, HttpResponse] = Hammock.request(Method.POST, Uri.unsafeParse(base + subpath), headers,
  //  Some(Entity.ByteArrayEntity)


  def getIO[T](subpath: String, headers: Map[String, String])(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T] =
    get(subpath, headers).as[T](D, M).exec[IO]

  def getAPI[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]) =
    getIO[T](api + subpath, headers)(D, M)

  def getStats: IO[Stats] = getIO[Stats](s"/engine/${version}/stats", Map.empty)

  def getVersion: IO[Version] = getIO[Version](s"/engine/${version}/version", Map.empty)

  def getEngineStatus = get(s"/engine/${version}/status", Map.empty).map(_.entity).exec[IO]


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
  def abort(id: String): IO[group.research.aging.cromwell.client.StatusInfo] =  getAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}/${id}/abort")

  def getCallOutputs(id: String): IO[CallOutputs] = getAPI[CallOutputs](s"/workflows/${version}/${id}/outputs")

  protected def queryString(status: WorkflowStatus = WorkflowStatus.AnyStatus): String = status match {
    case WorkflowStatus.AnyStatus => s"/workflows/${version}/query"
    case status: WorkflowStatus =>   s"/workflows/${version}/query?status=${status.entryName}"
  }

  def getQuery(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[QueryResults] = {
    val url = queryString(status)
    getAPI[QueryResults](url)
  }

  def getAllCallOutputs(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[List[CallOutputs]] =
    getQuery(status).flatMap(q=>
      q.results.map(r=>this.getCallOutputs(r.id)).sequence
    )


  def getLogs(id: String): IO[Logs] = getAPI[Logs](s"/workflows/${version}/${id}/logs")

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.AnyStatus) = getQuery(status).flatMap(q=>
    q.results.map(r=>this.getLogs(r.id)).sequence
  )

  def getBackends: IO[Backends] = getAPI[Backends](s"/workflows/${version}/backends")

  def getMetadata(id: String, v: String = "v2", expandSubWorkflows: Boolean = true): IO[Metadata] = getAPI[Metadata](s"/workflows/${v}/${id}/metadata?expandSubWorkflows=${expandSubWorkflows}")

  def getAllMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus) = getQuery(status).flatMap(q=>
    q.results.map(r=>this.getMetadata(r.id)).sequence
  )

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
