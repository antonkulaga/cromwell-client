package group.research.aging.cromwell.web.communication

import akka.actor.ActorRef
import group.research.aging.cromwell.web._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.syntax._


object WebsocketMessages {
  sealed trait SocketMessage
  case object WsHandleDropped extends SocketMessage
  case class ConnectWsHandle(ref: ActorRef) extends SocketMessage

  object WebsocketMessage
  @JsonCodec sealed trait WebsocketMessage extends SocketMessage
  case object EmptyWebsocketMessage extends WebsocketMessage
  case object WebsocketOpened extends WebsocketMessage
  case object WebsocketClosed extends WebsocketMessage
  object WebsocketAction {lazy val empty = WebsocketAction(EmptyAction) }
  case class WebsocketAction(action: Action) extends WebsocketMessage

}

