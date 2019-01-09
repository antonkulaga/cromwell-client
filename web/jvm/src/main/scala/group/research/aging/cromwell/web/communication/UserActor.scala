package group.research.aging.cromwell.web.communication
import akka.actor.{Actor, ActorRef}
import group.research.aging.cromwell.client.{CromwellClient, Metadata, StatusInfo, WorkflowStatus}
import group.research.aging.cromwell.web.{Commands, EmptyAction, Messages, Results}
import wvlet.log.LogSupport
import akka.pattern._
import group.research.aging.cromwell.web.Commands.{ChangeClient, StreamMetadata}
import group.research.aging.cromwell.web.communication.WebsocketMessages.WebsocketAction
import akka.pattern.pipe
import scala.concurrent.Future


case class UserActor(username: String) extends Actor with LogSupport {

  debug(s"user actor ${username}")

//  val generator = new CentromereGenerator


  protected def operation(output: List[ActorRef], client: CromwellClient): Receive = {
    case WebsocketMessages.ConnectWsHandle(ref) =>
      this.context.become(operation(output :+ ref, client))
      ref ! WebsocketMessages.WebsocketAction(Results.UpdatedClient(client))

    case WebsocketMessages.WsHandleDropped =>
      info("Websocket HANDLER DROP!")

    case WebsocketMessages.WebsocketAction(action) =>
      debug(s"WebsocketAction: \n ${action}")
      self ! action


    case Commands.GetMetadata(status) =>
      debug("GET METADATA!")
      val metaFut: Future[Results.UpdatedMetadata] = client.getAllMetadata(status).map(m=> Results.UpdatedMetadata(m)).unsafeToFuture()
      pipe(metaFut)(context.dispatcher) to self


    case Commands.CleanMessages =>
      this.context.become(operation(output, client))

    case Commands.Run(wdl, input, options) =>
      val postFut = client.postWorkflowStrings(wdl, input, options).map(s=>Results.WorkflowSent(s))(context.dispatcher)
      pipe(postFut)(context.dispatcher) to self

    case Results.WorkflowSent(status) =>
      val metaFut: Future[Results.UpdatedMetadata] = client.getAllMetadata(WorkflowStatus.AnyStatus).map(m=> Results.UpdatedMetadata(m)).unsafeToFuture()
      pipe(metaFut)(context.dispatcher) to self

    case ChangeClient(newURL) =>
      debug(s"CHANGE CLIENT to ${newURL}!")
      val newClient = client.copy(base = newURL)
      this.context.become(operation(output, newClient))

    case Commands.Abort(id) =>
      val ab: Future[Commands.GetMetadata] =  client.abort(id).map(_=>Commands.GetMetadata(WorkflowStatus.AnyStatus)).unsafeToFuture()
      pipe(ab)(context.system.dispatcher).pipeTo(self)


    case e: Messages.Errors =>
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(e))

    case akka.actor.Status.Failure(th) =>
      self !  Messages.Errors(Messages.ExplainedError(s"running workflow at ${client.base} failed", Option(th.getMessage).getOrElse(""))::Nil)


    case stream @ StreamMetadata(status, id) =>
      error("streams are not implemented yet!")
      /*
      val query = client.getQuery(status).unsafeRunSync()
      val ids = query.results.map(_.id)
      for(id < ids)
        {
          pipe(client.getMetadata(id).unsafeToFuture())
          //output.foreach(o=>o ! WebsocketMessages.WebsocketAction())
        }
      */

    case EmptyAction =>
      debug("empty action, no reaction")

    case other =>
      error("UNKNOWN MESSAGE:\n" + other)

  }

  override def receive: Receive = operation(Nil, new CromwellClient(scala.util.Properties.envOrElse("CROMWELL", "http://localhost:8000" ))
  )

}