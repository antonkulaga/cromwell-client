package group.research.aging.cromwell.client

import cats.effect.IO
import cats.free.Free
import cats.implicits._
import hammock._
import hammock.circe.implicits._
import hammock.marshalling._
import io.circe.generic.auto._

trait CromwellClientShared extends RosHttp with CromwellClientLike {

  def base: String
  def version: String

  lazy val baseNoPort: String = {
    val first = base.indexOf(":/")
    val last = base.lastIndexOf(":")
    if(first!=last) base.substring(0, last) else base
  }

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

  def get(subpath: String, headers: Map[String, String]): Free[HttpF, HttpResponse] =
    Hammock.request(Method.GET, Uri.unsafeParse(base + subpath), headers)

  //def post[T](subpath: String, headers: Map[String, String], body: T): Free[HttpF, HttpResponse] = Hammock.request(Method.POST, Uri.unsafeParse(base + subpath), headers,
  //  Some(Entity.ByteArrayEntity)


  def getIO[T](subpath: String, headers: Map[String, String])(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T] =
    get(subpath, headers).as[T](D, M).exec[IO]

  def getAPI[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T] =
    getIO[T](api + subpath, headers)(D, M)

  def getStats: IO[Stats] = getIO[Stats](s"/engine/${version}/stats", Map.empty)

  def getVersion: IO[Version] = getIO[Version](s"/engine/${version}/version", Map.empty)

  def getEngineStatus: IO[Entity] = get(s"/engine/${version}/status", Map.empty).map(_.entity).exec[IO]


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

  protected def queryString(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): String = status match {
    case WorkflowStatus.AnyStatus => s"/workflows/${version}/query?includeSubworkflows=${includeSubworkflows}"
    case status: WorkflowStatus =>   s"/workflows/${version}/query?status=${status.entryName}&includeSubworkflows=${includeSubworkflows}"
  }

  def getQuery(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): IO[QueryResults] = {
    val url = queryString(status, includeSubworkflows)
    getAPI[QueryResults](url)
  }

  def getAllCallOutputs(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): IO[List[CallOutputs]] =
    getQuery(status, includeSubworkflows).flatMap(q=>
      q.results.map(r=>this.getCallOutputs(r.id)).sequence
    )


  def getLogs(id: String): IO[Logs] = getAPI[Logs](s"/workflows/${version}/${id}/logs")

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[List[Logs]] = getQuery(status).flatMap(q=>
    q.results.map(r=>this.getLogs(r.id)).sequence
  )

  def getBackends: IO[Backends] = getAPI[Backends](s"/workflows/${version}/backends")

  def getMetadata(id: String, v: String = "v2", expandSubWorkflows: Boolean = true): IO[Metadata] =
    getAPI[Metadata](s"/workflows/${v}/${id}/metadata?expandSubWorkflows=${expandSubWorkflows}")

  def getAllMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[List[Metadata]] = getQuery(status, false).flatMap(q=>
    q.results.map(r=>this.getMetadata(r.id)).sequence
  )

}