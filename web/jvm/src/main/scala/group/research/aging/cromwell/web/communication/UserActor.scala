package group.research.aging.cromwell.web.communication
import akka.actor.{Actor, ActorRef}
import wvlet.log.LogSupport



case class UserActor(username: String) extends Actor with LogSupport {

  debug(s"user actor ${username}")


//  val generator = new CentromereGenerator


  protected def operation(output: List[ActorRef]): Receive = {
    case WebsocketMessages.ConnectWsHandle(ref) =>
      this.context.become(operation(output :+ ref))

    case WebsocketMessages.WsHandleDropped =>
      info("WD HANDLER DROP!")

//    case WebsocketMessages.WebsocketAction(null) =>

  }

  override def receive: Receive = operation(Nil)

}