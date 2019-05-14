package group.research.aging.cromwell.web.api.runners

import akka.actor.{Actor, ActorRef, Props, _}
import akka.http.scaladsl.HttpExt
import group.research.aging.cromwell.client.CromwellClientAkka
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.common.BasicActor
import wvlet.log.LogSupport

import scala.concurrent.duration._


case class RunnerManager(http: HttpExt) extends BasicActor {

  protected def operation(workers: Map[String, ActorRef]): Receive = {
    case mes @ MessagesAPI.ServerCommand(com, serverURL, callbackURLs, _, authOpt) =>
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
          context.become(operation(workers.updated(serverURL, context.actorOf(Props(new RunnerWorker(client)), name = "runner_" + workers.size + 1))))
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
