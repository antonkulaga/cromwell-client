package group.research.aging.cromwell.web.api.runners

import akka.http.scaladsl.model.DateTime
import group.research.aging.cromwell.client.{CallOutput, CallOutputs, StatusInfo, WorkflowStatus}
import group.research.aging.cromwell.web.Commands
import io.circe._
import io.circe.generic.JsonCodec

import scala.concurrent.duration._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras._
import io.circe.syntax._

import scala.concurrent.duration._

object MessagesAPI {

  lazy val defaultDuration: FiniteDuration = 48 hours

  trait MessageAPI

  @JsonCodec case class PipelineResult(id: String, status: String, outputs: Map[String,  CallOutput], inputs: Option[Json] = None) extends MessageAPI


  object CallBack {
    val defaultUpdateOn: Set[WorkflowStatus] = Set(
      WorkflowStatus.Aborted,
      WorkflowStatus.Failed,
      WorkflowStatus.Succeeded
    )
    val defaultUpdateOnStrings = defaultUpdateOn.map(_.entryName)
  }
  case class CallBack(backURL: String, workflowId: String, serverURL: String,
                      updateOn: Set[String] = CallBack.defaultUpdateOnStrings, giveInputs: Boolean,
                      start: DateTime = DateTime.now, timeout: Duration = 48 hours, headers: Map[String, String] = Map.empty) extends MessageAPI

  case object Poll extends MessageAPI

  object ServerCommand {
    def emptyWith(serverURL: String, callbackURLs: Set[String] = Set.empty): ServerCommand = {
      ServerCommand(Commands.EmptyCommand, serverURL, callbackURLs, false)
    }
  }

  /**
    * The command to execute on a server and callback the result to appropriate place
    * @param command
    * @param serverURL
    * @param callbackURLs
    * @param withInputs
    * @param extraHeaders
    */
  case class ServerCommand(command: Commands.Command, serverURL: String,
                           callbackURLs: Set[String] = Set.empty,
                           withInputs: Boolean = false,
                           extraHeaders: Map[String, String] = Map.empty) extends MessageAPI
  {
    def callbacks(id: String): Set[CallBack] = callbackURLs.map(u=>
      CallBack(u, id, serverURL, CallBack.defaultUpdateOnStrings, withInputs, DateTime.now, defaultDuration, extraHeaders))
    def promise(status: StatusInfo, additionalParameters: Map[String, String] = Map.empty[String, String]): ServerPromise = ServerPromise(status, callbacks(status.id))
  }

  case class ServerPromise(statusInfo: StatusInfo, callbacks: Set[CallBack]) extends MessageAPI

  //case class PollCallbacks(callBacks: List[CallBack]) extends MessageAPI
}
