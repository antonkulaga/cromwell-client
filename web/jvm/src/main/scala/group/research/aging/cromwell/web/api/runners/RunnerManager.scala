package group.research.aging.cromwell.web.api.runners

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.HttpExt
import group.research.aging.cromwell.client.CromwellClientAkka
import group.research.aging.cromwell.web.{Commands, Results}
import wvlet.log.LogSupport

/**
  * Actors that proccesses most of websocket messages from the users and back
 *
  * @param username
  */
case class RunnerManager(http: HttpExt) extends Actor with LogSupport {

  override def preStart { debug(s"runner manager actor started at ${java.time.LocalDateTime.now()}") }
  override def postStop { debug(s"runner manager actor stopped at ${java.time.LocalDateTime.now()}") }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    error(s"runner manager actor restarted at ${java.time.LocalDateTime.now()}")
    error(s" MESSAGE: ${message.getOrElse("")}")
    error(s" REASON: ${reason.getMessage}")
    super.preRestart(reason, message)
  }

  protected def operation(workers: Map[String, ActorRef]): Receive = {
    case mes @ MessagesAPI.ServerCommand(com, serverURL, callbackURLs) =>
      workers.get(serverURL) match {
        case Some(worker) =>
          com match {
            case run: Commands.Run =>
              debug(s"sending run message wrapped in server message to ${serverURL} with callbacks to ${callbackURLs}")
              worker forward mes

            case run: Commands.TestRun =>
              debug(s"sending TEST MESSAGE wrapped in server message to ${serverURL} with callbacks to ${callbackURLs}")
              worker forward mes

            case other =>
              debug(s"sendins message to ${serverURL}")
              debug(other)
              worker forward other
          }

        case None =>
          val client  = CromwellClientAkka(serverURL, "v1", http)
          debug(s"adding client for ${serverURL}")
          context.become(operation(workers.updated(serverURL, context.actorOf(Props(new RunnerWorker(client))))))
          self forward mes
      }

  }

  /*

  protected def operation(servers: Map[String, CromwellClientAkka],
                           callbacks: Map[String, Set[MessagesAPI.CallBack]]): Receive = {

    case MessagesAPI.ServerPromise(statusInfo, callbacks) =>

    case mes @ MessagesAPI.ServerCommand(Commands.Run(wdl, input, options), serverURL, callbackURL) =>
      servers.get(serverURL) match {
        case Some(client) =>
          val postFut = client.postWorkflowStrings(wdl, input, options).map{s=>
            (mes.callbackURLs, Results.WorkflowSent(s))
          }(context.dispatcher)
          pipe(postFut)(context.dispatcher) to sender

        case None =>
          val updatesServers: Map[String, CromwellClientAkka] = if(servers.contains(serverURL))
            servers else
            servers.updated(serverURL, CromwellClientAkka(serverURL, "v1", http))
          context.become(operation(updatesServers, callbacks))
          debug(s"Server URL does not exist, creating a connection")
          self forward mes
      }




    case callback @ MessagesAPI.CallBack(back, id, serverURL, _, _) =>
      //val updatedServers = if(servers.contains(serverURL)) servers else servers + (serverURL -> new CromwellClient(serverURL))
      //val client = CromwellClientAkka(serverURL, "v1", http)
      val updatesServers = if(servers.contains(serverURL)) servers else servers.updated(serverURL, CromwellClientAkka(serverURL, "v1", http))
      val updatedCallbacks = if(callbacks.contains(serverURL)) callbacks.updated(serverURL, callbacks(serverURL) + callback) else callbacks.updated(serverURL, Set(callback))
      debug(s"adding callback ${callback}")
      //this.context.become(operation(updatedCallbacks))

  }
  */

  override def receive: Receive = operation(Map.empty)

}
