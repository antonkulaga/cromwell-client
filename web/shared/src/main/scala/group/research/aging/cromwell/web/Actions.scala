package group.research.aging.cromwell.web

import group.research.aging.cromwell.client._
import io.circe.generic.JsonCodec
import java.util.UUID

import cats.Monoid
import group.research.aging.cromwell.client.WorkflowStatus.AnyStatus

object Action
@JsonCodec sealed trait Action

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

  object UpdatedStatus {
    implicit def monoid: cats.Monoid[UpdatedStatus] = new Monoid[UpdatedStatus] {
      override def empty: UpdatedStatus = UpdatedStatus(WorkflowStatus.AnyStatus)

      override def combine(x: UpdatedStatus, y: UpdatedStatus): UpdatedStatus = y //ugly but works
    }
  }
  case class UpdatedStatus(info: WorkflowStatus) extends ActionResult
  case class UpdatedMetadata(metadata: Map[String, Metadata]) extends ActionResult
  case class UpdateClient(serverURL: String) extends ActionResult
  case class ServerResult(action: Action) extends ActionResult
  case class WorkflowSent(status: StatusInfo) extends ActionResult
  case class WorkflowValidated(validation: ValidationResult) extends ActionResult

  object QueryWorkflowResults {
    lazy val empty = QueryWorkflowResults(QueryResults.empty, Map.empty)
  }
  case class QueryWorkflowResults(queryResults: QueryResults, metadata: Map[String, Metadata], limit: Int = 25, offset: Int = 0) extends ActionResult {
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
      this.copy(queryResults.copy(results = newResults.slice(offset, offset + limit)), offset = offset, limit = limit)
    }
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
  case class QueryWorkflows(status: WorkflowStatus, expandSubworkFlows: Boolean = true, limit: Int = 50, offset: Int = 0) extends Command
  case class GetQuery(statusS: WorkflowStatus, includeSubworkflows: Boolean = true) extends Command
  case class UpdateStatus(status: WorkflowStatus)  extends Command
  //case class Paginate(limit: Int, offset: Int) extends Command
  case class ChangeClient(newURL: String) extends Command
  case class Run(wdl: String, input: String, options: String = "", dependencies: List[(String, String)] = Nil) extends Command
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
