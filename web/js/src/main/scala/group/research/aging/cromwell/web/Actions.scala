package group.research.aging.cromwell.web

import diode.Action
import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{CromwellClientLike, Metadata, StatusInfo, WorkflowStatus}

object Messages {
  class Error(errorMessage: String) extends Action
  case class ExplainedError(message: String, errorMessage: String) extends Error(errorMessage)
  case class Errors(errors: List[ExplainedError]) extends Action

}

object Results {
  case class UpdatedStatus(info: StatusInfo) extends Action
  case class UpdatedMetadata(metadata: List[Metadata]) extends Action
  case class UpdatedClient(client: CromwellClientLike) extends Action
}

object Commands{

  case class GetMetadata(status: WorkflowStatus = WorkflowStatus.AnyStatus) extends Action
  case class ChangeClient(newURL: String) extends Action
  case class Run(wdl: String, options: String, input: String) extends Action
  case object LoadLastUrl extends LoadKey("lastURL")

  abstract class LoadKey(val key: String) extends Action
}

