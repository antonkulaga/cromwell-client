package group.research.aging.cromwell.web

import diode.Action
import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{Metadata, Status, WorkflowStatus}

object Results {
  case class UpdatedMetadata(metadata: List[Metadata]) extends Action
  case class UpdatedClient(client: CromwellClient) extends Action
}

object Commands{

  case class GetMetadata(status: WorkflowStatus = WorkflowStatus.Undefined) extends Action
  case class ChangeClient(newURL: String) extends Action
  case class Run(wdl: String, options: String, input: String) extends Action
  case object LoadLastUrl extends LoadKey("lastURL")

  abstract class LoadKey(val key: String) extends Action
}

