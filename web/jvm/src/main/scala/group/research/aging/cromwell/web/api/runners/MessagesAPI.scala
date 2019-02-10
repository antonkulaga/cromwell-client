package group.research.aging.cromwell.web.api.runners

import akka.http.scaladsl.model.DateTime
import group.research.aging.cromwell.client.StatusInfo
import group.research.aging.cromwell.web.Commands

import scala.concurrent.duration._

object MessagesAPI {

  lazy val defaultDuration: FiniteDuration = 48 hours

  trait MessageAPI

  case class CallBack(backURL: String, workflowId: String, serverURL: String, start: DateTime = DateTime.now, timeout: Duration = 48 hours) extends MessageAPI

  case object Poll extends MessageAPI

  object ServerCommand {
    def emptyWith(serverURL: String, callbackURLs: Set[String] = Set.empty): ServerCommand = {
      ServerCommand(Commands.EmptyCommand, serverURL, callbackURLs)
    }
  }

  case class ServerCommand(command: Commands.Command, serverURL: String, callbackURLs: Set[String] = Set.empty) extends MessageAPI
  {
    def callbacks(id: String): Set[CallBack] = callbackURLs.map(u=> CallBack(u, id, serverURL, DateTime.now, defaultDuration))
    def promise(status: StatusInfo): ServerPromise = ServerPromise(status, callbacks(status.id))
  }

  case class ServerPromise(statusInfo: StatusInfo, callbacks: Set[CallBack]) extends MessageAPI

  //case class PollCallbacks(callBacks: List[CallBack]) extends MessageAPI
}
