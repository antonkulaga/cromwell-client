package group.research.aging.cromwell.web.api.runners

import akka.actor.{Actor, ActorRef, Props, _}
import akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer
import group.research.aging.cromwell.client.CromwellClientAkka
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.common.BasicActor
import wvlet.log.LogSupport

import scala.concurrent.duration._


case class RunnerManager(implicit http: HttpExt, materializer: ActorMaterializer) extends BasicActor {

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
          val client  = CromwellClientAkka(serverURL, "v1")
          debug(s"adding client for ${serverURL}")
          context.become(operation(workers.updated(serverURL, context.actorOf(Props(new RunnerWorker(client)), name = "runner_" + workers.size + 1))))
          self forward mes
      }

  }

  override def receive: Receive = operation(Map.empty)

}
