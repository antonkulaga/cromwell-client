package group.research.aging.cromwell.web.api.runners

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.HttpExt
import akka.stream.{ActorMaterializer, Materializer}
import group.research.aging.cromwell.client.{CromwellClientAkka, QueryResults, WorkflowStatus}
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.Commands.BatchRun
import group.research.aging.cromwell.web.Results.QueryWorkflowResults
import group.research.aging.cromwell.web.api.runners.MessagesAPI.ServerCommand
import group.research.aging.cromwell.web.common.BasicActor
import group.research.aging.cromwell.web.util.HostExtractor

case class RunnerManager(implicit http: HttpExt, materializer: ActorMaterializer) extends BasicActor with HostExtractor {

  protected def operation(workers: Map[String, ActorRef], batch: BatchRun): Receive = {

    case MessagesAPI.ServerResults(server, queryResults) =>
      //debug(s"RESULTS updates for ${server}")
      workers.get(server).orElse(workers.get(processHost(server))) match {
        case Some(inf) =>
         // val updatedInfo = workers.updated(server, inf.copy(queryResults = inf.queryResults))
          val runSubmit = queryResults.results.filter(v=>v.status == WorkflowStatus.Running.entryName || v.status == WorkflowStatus.Submitted.entryName)
          val topRunSubmit = runSubmit.filter(v=>v.parentWorkflowId.isEmpty )
          debug(s"top status: ${topRunSubmit.toString()} \n batch size = ${batch.inputs.size}")
          if(batch.nonEmpty && topRunSubmit.isEmpty) {
            debug(s"SENDING BATCH RUN ${batch.title} (${batch.inputs.length - 1} remains ) WITH ${batch.head}")
            //inf.worker ! batch.head
            self ! MessagesAPI.ServerCommand(batch.head, server)
            context.become(operation(workers, batch.tail))
          }

        case None=>

          error(s"NO WORKER DETECTED FOR ${server}")
      }

    case b: Commands.BatchRun =>
      pprint.pprintln(s"batch task, avaliable servers are: ${workers.keys.mkString(", ")}")
      val newServers: Set[String] = b.servers.map(processHost).toSet -- workers.keySet
      if(newServers.isEmpty){
        context.become(operation(workers, b))
      } else {
        debug("sending batch tasks to new servers: " + newServers.mkString(", "))
        val zp = newServers.zip(b.runs)
        context.become(operation(workers, b.copy(inputs = b.inputs.drop(zp.size))))
        println(s"changing batch size from ${b.inputs.size} to ${b.inputs.drop(zp.size).size}")
        for{
          (s, r) <- zp
        } self ! MessagesAPI.ServerCommand(r, s)

      }
      println("returning batch to server")
      sender() ! b //TODO: for debugging


    case mes @ MessagesAPI.ServerCommand(com, serverURL, callbackURLs, _, _, _) =>
      workers.get(serverURL).orElse(workers.get(processHost(serverURL))) match {
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
          val newURL = processHost(serverURL)
          if(newURL!=serverURL) debug(s"adding client for ${serverURL} which becomes ${newURL} after host substitution!")
          else debug(s"adding client for ${serverURL}")
          val client  = CromwellClientAkka(newURL, "v1")
          val a: ActorRef = context.actorOf(Props(new RunnerWorker(client)), name = "runner_" + workers.size + 1)
          //val info = WorkerInformation(serverURL, a)
          context.become(operation(workers.updated(newURL, a), batch))
          println("forwarding servermessage to self")
          self forward mes
      }

  }

  override def receive: Receive = operation(Map.empty, BatchRun.empty)

}
