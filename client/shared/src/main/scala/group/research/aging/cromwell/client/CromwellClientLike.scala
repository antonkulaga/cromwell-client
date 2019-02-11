package group.research.aging.cromwell.client

import cats.effect.IO
import cats.free.Free
import fr.hmil.roshttp.body.JSONBody.JSONObject
import hammock.{Decoder, _}
import hammock.marshalling._

import scala.concurrent.Future

trait CromwellClientLike {

  def base: String
  def version: String

  def api: String

  def makeWorkflowOptions(output: String, log: String="", call_log: String = ""): String

  def get(subpath: String, headers: Map[String, String]): Free[HttpF, HttpResponse]

  def getIO[T](subpath: String, headers: Map[String, String])(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T]

  def getAPI[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T]

  def getStats: IO[Stats]

  def getVersion: IO[Version]

  def getEngineStatus: IO[Entity]

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
  def abort(id: String): IO[group.research.aging.cromwell.client.StatusInfo]

  def getOutput(id: String): IO[CallOutputs]

  def getQuery(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): IO[QueryResults]

  def getAllOutputs(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): IO[List[CallOutputs]]

  def getLogs(id: String): IO[Logs]

  def getAllLogs(status: WorkflowStatus = WorkflowStatus.AnyStatus): IO[List[Logs]]

  def getBackends: IO[Backends]

  def getMetadata(id: String, v: String = "v2", expandSubWorkflows: Boolean = true): IO[Metadata]

  def getAllMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = true): IO[List[Metadata]]


  def postWorkflowStrings(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String,
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                         ): Future[group.research.aging.cromwell.client.StatusInfo]

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
                   workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[group.research.aging.cromwell.client.StatusInfo]
}
