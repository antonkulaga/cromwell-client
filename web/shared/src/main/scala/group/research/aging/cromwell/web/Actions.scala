package group.research.aging.cromwell.web

import group.research.aging.cromwell.client._
import io.circe.generic.JsonCodec

object Action
trait Action
case object EmptyAction extends EmptyAction
trait EmptyAction extends Action

object Messages {
  trait Message extends Action
  case object EmptyMessage extends Message with EmptyAction
  class Error(errorMessage: String) extends Message
  @JsonCodec case class ExplainedError(message: String, errorMessage: String) extends Error(errorMessage)
  @JsonCodec case class Errors(errors: List[ExplainedError]) extends Message

}

object Results {
  trait ActionResult extends Action
  case object EmptyResult extends ActionResult with EmptyAction
  @JsonCodec case class UpdatedStatus(info: StatusInfo) extends ActionResult
  @JsonCodec case class UpdatedMetadata(metadata: List[Metadata]) extends ActionResult
  @JsonCodec case class UpdatedClient(client: CromwellClient) extends ActionResult
}

object Commands{

  trait Command extends Action
  case object EmptyCommand extends EmptyAction with Command
  //@JsonCodec
  case class GetMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus) extends Command
  @JsonCodec case class ChangeClient(newURL: String) extends Command
  @JsonCodec case class Run(wdl: String, options: String, input: String) extends Command
  case object LoadLastUrl extends LoadKey("lastURL") with Command
  @JsonCodec case class UpdateURL(url: String) extends Command
  abstract class LoadKey(val key: String) extends Command
}
