package group.research.aging.cromwell.web.communication
import akka.actor.{Actor, ActorRef}
import group.research.aging.cromwell.client.{CromwellClient, Metadata}
import group.research.aging.cromwell.web.{Commands, Results}
import group.research.aging.cromwell.web.Results.ActionResult
import wvlet.log.LogSupport
import akka.pattern._

import scala.concurrent.Future


case class UserActor(username: String) extends Actor with LogSupport {

  debug(s"user actor ${username}")

//  val generator = new CentromereGenerator


  protected def operation(output: List[ActorRef], client: CromwellClient = CromwellClient.localhost): Receive = {
    case WebsocketMessages.ConnectWsHandle(ref) =>
      this.context.become(operation(output :+ ref))

    case WebsocketMessages.WsHandleDropped =>
      info("WD HANDLER DROP!")

    case WebsocketMessages.WebsocketAction(Commands.GetMetadata(status)) =>
      debug("GET METADATA!")
      val metaFut = client.getAllMetadata(status).map(m=>Results.UpdatedMetadata(m)).unsafeToFuture()
      pipe(metaFut)(context.dispatcher) to self

    case WebsocketMessages.WebsocketAction(Commands.CleanMessages) =>
      this.context.become(operation(output, client))

    case u @ Results.UpdatedMetadata(m) =>
      debug("RECEIVED METADATA: ")
      debug(u)
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(u))

    case other =>
      error("UNKNOWN MESSAGE:\n" + other)

  }

  override def receive: Receive = operation(Nil)

}