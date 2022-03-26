package group.research.aging.cromwell.web

import group.research.aging.cromwell.client._
import io.circe.generic.JsonCodec
import java.util.UUID

import cats.Monoid
import group.research.aging.cromwell.client.WorkflowStatus.AnyStatus

import scala.collection.SortedSet

object Action
@JsonCodec sealed trait Action

trait WorkflowQueryLike {
  def limit: Int
  def offset: Int
  def status: WorkflowStatus
  def expandSubworkflows: Boolean
}

object KeepAlive {
  lazy val web: Action = KeepAlive("web")
}
case class KeepAlive(name: String) extends Action
case object EmptyAction extends Action with EmptyAction
trait EmptyAction {
  self: Action =>
}

object Messages {
  object Message
  @JsonCodec  sealed trait Message extends Action

  case object EmptyMessage extends Message with EmptyAction
  //class Error(val errorMessage: String) extends Message
  trait Error{
    self: Message =>
    def message: String
  }
  //@JsonCodec case class ExplicitError(errorMessage: String) extends Error
  trait ExplainedMessage {
    self: Message =>

    def title: String
    def message: String
  }

  @JsonCodec case class ExplainedError(title: String, message: String) extends Error with Message with ExplainedMessage
  @JsonCodec case class Errors(errors: List[ExplainedError]) extends Message
  @JsonCodec case class Infos(infos: List[Info]) extends Message
  @JsonCodec case class Info(title: String, message: String) extends Message with ExplainedMessage

}

object Results {
  object ActionResult
  @JsonCodec sealed trait ActionResult extends Action
  case object EmptyResult extends ActionResult with EmptyAction
  case class GeneratedSequence(sequence: String) extends ActionResult

  /*
  object UpdatedStatus {
    implicit def monoid: cats.Monoid[UpdatedStatus] = new Monoid[UpdatedStatus] {
      override def empty: UpdatedStatus = UpdatedStatus(WorkflowStatus.AnyStatus)

      override def combine(x: UpdatedStatus, y: UpdatedStatus): UpdatedStatus = y //ugly but works
    }
  }
  case class UpdatedStatus(info: WorkflowStatus) extends ActionResult
  */
  case class UpdatedMetadata(metadata: Map[String, Metadata]) extends ActionResult
  case class UpdateClient(serverURL: String, fileUrlPrefix: Option[String] = None) extends ActionResult
  case class UpdatePipelines(pipelines: Pipelines) extends ActionResult
  case class ServerResult(action: Action) extends ActionResult
  case class WorkflowSent(status: StatusInfo) extends ActionResult
  case class WorkflowValidated(validation: ValidationResult) extends ActionResult

  object QueryWorkflowResults {
    lazy val empty = QueryWorkflowResults(QueryResults.empty, Map.empty, WorkflowStatus.AnyStatus, true, 25, 0)
  }
  case class QueryWorkflowResults(queryResults: QueryResults,
                                  metadata: Map[String, Metadata],
                                  status: WorkflowStatus,
                                  expandSubworkflows: Boolean,
                                  limit: Int,
                                  offset: Int) extends ActionResult with WorkflowQueryLike {
    lazy val complete: Boolean = queryResults.ids == metadata.keySet
    lazy val missing: Set[String] = queryResults.ids.diff(metadata.keySet)

    lazy val loaded: (Int, Int) = (metadata.size, queryResults.ids.size)

    def updated(upd: UpdatedMetadata): QueryWorkflowResults = {
      copy(metadata = metadata -- upd.metadata.keys ++ upd.metadata)
    }

    def paginate(limit: Int, offset: Int): QueryWorkflowResults = {
      val (unstarted, started) = queryResults.results.partition(_.start.isEmpty)
      val newResults = unstarted ++ started.sortWith{
        case (a, b) => a.start.get.isAfter(b.start.get)
      }
      this.copy(queryResults.copy(results = newResults.slice(offset, offset + limit)), limit = limit, offset = offset)
    }

    lazy val workflowMetadataResults: List[WorkflowNode] = WorkflowNode.fromMetadata(metadata)
  }

}

object Commands{
  object Command
  @JsonCodec sealed trait Command extends Action
  case object CheckTime extends Command

  case object EmptyCommand extends EmptyAction with Command
  case class SendToServer(action: Action) extends Command
  case object CleanMessages extends Command

  case class StreamMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus, id: String = UUID.randomUUID().toString) extends Command
  object QueryWorkflows {
    lazy val default: QueryWorkflows = QueryWorkflows(WorkflowStatus.AnyStatus, expandSubworkflows = true, 25, 0)
    def apply(query: WorkflowQueryLike): QueryWorkflows = QueryWorkflows(
      status = query.status, expandSubworkflows = query.expandSubworkflows, limit = query.limit, offset = query.offset
    )

    implicit def monoid: cats.Monoid[QueryWorkflows] = new Monoid[QueryWorkflows] {
      override def empty: QueryWorkflows = QueryWorkflows.default

      override def combine(x: QueryWorkflows, y: QueryWorkflows): QueryWorkflows = y //ugly but works
    }
  }
  case class QueryWorkflows(status: WorkflowStatus, expandSubworkflows: Boolean = true, limit: Int = 25, offset: Int = 0) extends Command with WorkflowQueryLike
  case class GetQuery(status: WorkflowStatus, includeSubworkflows: Boolean = true) extends Command
  case class UpdateStatus(status: WorkflowStatus)  extends Command
  case class SelectPipeline(name: String) extends Command
  //case class Paginate(limit: Int, offset: Int) extends Command
  case class ChangeClient(newURL: String) extends Command
  case class Run(wdl: String, input: String, options: String = "", dependencies: List[(String, String)] = Nil) extends Command
  object BatchRun {
    lazy val empty: BatchRun = BatchRun(Pipeline.empty, Nil, Nil, "", "", 0, 0)
  }
  @JsonCodec case class BatchRun(pipeline: Pipeline,
                                 experiments: Seq[String],
                                 servers: Seq[String],
                                 title: String = "",
                                 options: String = "",
                                 current: Int = 0,
                                 maxBatchSize: Int = 5
                               //  minimalBatch: Int = 2 //JUST TO AVOID STARTING BY 1 each time
                                ) extends Command {
    /*
    lazy val runs: Seq[Run] = inputs.map(i => Run(wdl, i, options, dependencies))
    lazy val head: Run = runs.head
    lazy val tail: BatchRun = if(isEmpty) this else BatchRun(wdl, inputs.tail, servers, title, options, dependencies)

    */
    def isEmpty: Boolean = experiments.isEmpty
    def nonEmpty: Boolean = experiments.nonEmpty
    lazy val experimentsLeft: Int = experiments.size - current
    lazy val status = s"${current} of ${experiments.length}"

    def slice(from: Int, until: Int): Run = {
      val ee = experiments.slice(from, until)
      val exp = ee.map(e=> "\"" + e + "\"").mkString(", ")
      val status = "[" + title + "_" + (from+1) +"-" + until + "|" + experiments.length + "]"
      val p_name = pipeline.name.replace(".wdl", "").replace(".cwl", "")
      val experimentsName = p_name match {
        case "quant_by_runs" => "runs"
        case "quant_run" => "runs"
        case "quantification" => "experiments"
        case "quant_sample" => "experiment"
        case _ =>
          "experiments"
      }
      val input = s"""{
                "${p_name}.${experimentsName}": [${exp}],
                "${p_name}.title": "${status}"
                }"""
      pipeline.to_run(input, options)
    }
    def hasNext: Boolean = current + 1 <= experiments.length
    def next(num: Int): (Run, BatchRun) = {
      val b = Math.min(Math.min(num, maxBatchSize), experiments.length - current)
      val end: Int = Math.min(current + b, experiments.length)
      (slice(current, end), this.copy(current = current + b))
    }
  }

  object TestRun
  {
    val id = "e442e52a-9de1-47f0-8b4f-e6e565008cf1-TEST"
  }
  case class TestRun(wdl: String, input: String, options: String = "", dependencies: List[(String, String)] = Nil) extends Command
  case class Validate(wdl: String, input: String, options: String = "", dependencies: List[(String, String)] = Nil) extends Command
  case class Abort(id: String) extends Command
  //case class UpdateURL(url: String) extends Command

  trait LoadKey {
    self: Command =>
    val key: String
  }
  case object LoadLastUrl extends LoadKey with Command
  {
    val key = "lastURL"
  }

  case class EvalJS(code: String) extends Command

  case object SingleWorkflow{
    sealed trait SingleWorkflowCommand extends Command
    case class GetOutput(id: String) extends SingleWorkflowCommand
    case class GetStatus(id: String) extends SingleWorkflowCommand
    case class GetMetadata(id: String) extends SingleWorkflowCommand
  }



  /*
  case object Workflow {
    trait SingleWorkflowCommand extends Command
    case class GetOutputs(id: String)
    case class GetMetadata(id: String)
  }
  */
}
