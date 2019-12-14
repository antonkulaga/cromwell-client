package group.research.aging.cromwell.web.api.runners

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.HttpExt
import akka.stream.{ActorMaterializer, Materializer}
import group.research.aging.cromwell.client.{CromwellClientAkka, QueryResults, WorkflowStatus}
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.Commands.BatchRun
import group.research.aging.cromwell.web.Results.QueryWorkflowResults
import group.research.aging.cromwell.web.common.BasicActor

case class WorkerInformation(server: String,
                           worker: ActorRef,
                           queryResults: QueryResults = QueryResults.empty)

case class RunnerManager(implicit http: HttpExt, materializer: ActorMaterializer) extends BasicActor {

  protected def operation(workers: Map[String, WorkerInformation], batch: BatchRun = BatchRun.empty): Receive = {

    case MessagesAPI.ServerResults(server, queryResults) =>
      debug(s"COMPLETED RESULTS for ${server}")
      val from = sender()

      workers.get(server) match {
        case Some(inf) =>
          val updatedInfo = workers.updated(server, inf.copy(queryResults = inf.queryResults))
          val top = queryResults.results.filter(v=>v.parentWorkflowId.isEmpty && (v.status == WorkflowStatus.Running.entryName || v.status == WorkflowStatus.Submitted.entryName))
          if(batch.nonEmpty && top.isEmpty) {
            debug(s"SENDING BATCH RUN WITH ${batch.head}")
            inf.worker ! batch.head
            operation(updatedInfo, batch.tail)
          } else operation(updatedInfo, batch)

        case None=>

          error(s"NO WORKER DETECTED FOR ${server}")
      }

    case Commands.BatchRun(wdl, inputs, options, dependencies) =>
      pprint.pprintln(s"batch task, avaliable servers are: ${workers.keys.mkString(", ")}")
      operation(workers, batch)


    case mes @ MessagesAPI.ServerCommand(com, serverURL, callbackURLs, _, authOpt) =>
      workers.get(serverURL) match {
        case Some(WorkerInformation(_, worker, _)) =>
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
          val a: ActorRef = context.actorOf(Props(new RunnerWorker(client)), name = "runner_" + workers.size + 1)
          val info = WorkerInformation(serverURL, a)
          context.become(operation(workers.updated(serverURL, info)))
          self forward mes
      }

  }

  override def receive: Receive = operation(Map.empty)

}
