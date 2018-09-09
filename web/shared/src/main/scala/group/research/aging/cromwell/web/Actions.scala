package group.research.aging.cromwell.web

import group.research.aging.cromwell.client._
import io.circe.generic.JsonCodec

object Action
@JsonCodec sealed trait Action

case object KeepAliveAction extends Action with  EmptyAction
case object EmptyAction extends Action with EmptyAction
trait EmptyAction {
  self: Action =>
}

object Messages {
  @JsonCodec  sealed trait Message extends Action

  case object EmptyMessage extends Message with EmptyAction
  //class Error(val errorMessage: String) extends Message
  trait Error{
    self: Message =>
    def errorMessage: String
  }
  //@JsonCodec case class ExplicitError(errorMessage: String) extends Error
  @JsonCodec case class ExplainedError(message: String, errorMessage: String) extends Error with Message
  @JsonCodec case class Errors(errors: List[ExplainedError]) extends Message

}

object Results {
  @JsonCodec sealed trait ActionResult extends Action
  case object EmptyResult extends ActionResult with EmptyAction
  case class GeneratedSequence(sequence: String) extends ActionResult
  case class UpdatedStatus(info: StatusInfo) extends ActionResult
  case class UpdatedMetadata(metadata: List[Metadata]) extends ActionResult
  case class UpdatedClient(client: CromwellClient) extends ActionResult

  case class ServerResult(action: Action) extends ActionResult

}

object Commands{

  @JsonCodec sealed trait Command extends Action
  case object EmptyCommand extends EmptyAction with Command
  case class SendToServer(action: Action) extends Command
  case object CleanMessages extends Command

  case class GetMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus) extends Command
  case class ChangeClient(newURL: String) extends Command
  case class Run(wdl: String, options: String, input: String) extends Command
  case class UpdateURL(url: String) extends Command

  trait LoadKey {
    self: Command =>
    val key: String
  }
  case object LoadLastUrl extends LoadKey with Command
  {
    val key = "lastURL"
  }
}
