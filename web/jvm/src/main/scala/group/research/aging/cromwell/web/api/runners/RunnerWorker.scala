package group.research.aging.cromwell.web.api.runners

import akka.actor.{Actor, ActorRef}
import akka.pattern._
import akka.stream.ActorMaterializer
import cats.effect.IO
import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.{CallOutput, CromwellClientAkka}
import group.research.aging.cromwell.web.server.WebServer.http
import group.research.aging.cromwell.web.{Commands, Results}
import hammock.akka.AkkaInterpreter
import hammock.circe.implicits._
import hammock.{ContentType, Hammock, Method, _}
import io.circe.Json
import io.circe.syntax._
import wvlet.log.LogSupport

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
class RunnerWorker(client: CromwellClientAkka) extends Actor with LogSupport {

  debug(s"runner worker for ${client.base} cromwell server started!")

  implicit def dispatcher: ExecutionContextExecutor = context.dispatcher

  context.system.scheduler.schedule(
    1 second,
    2 seconds,
    self,
    MessagesAPI.Poll)

  implicit val materializer: ActorMaterializer = ActorMaterializer()(context.system)

  //implicit  protected def getInterpreter: Interpreter[IO] = Interpreter[IO]
  implicit protected def getInterpreter: AkkaInterpreter[IO] =
    new AkkaInterpreter[IO](http)

  protected def operation(callbacks: Map[String, Set[MessagesAPI.CallBack]]): Receive = {
    case MessagesAPI.Poll =>
      //debug(s"polling the callbacks, currently ${callbacks.mkString(",")} are avaliable")
      val results = client.getQuery(includeSubworkflows = true).unsafeRunSync().results
      val toDelete: Seq[(String, MessagesAPI.CallBack)] = for{
        r <- results
        if callbacks.contains(r.id)
        cb: MessagesAPI.CallBack <- callbacks(r.id)
        if cb.updateOn.contains(r.status)
      }
      yield {
        val outputs = client.getOutput(r.id).unsafeRunSync()
         val json: Json = MessagesAPI.PipelineResult(r.id, r.status, outputs.outputs).asJson
         val req = Hammock.request[Json](Method.POST, uri"${cb.backURL}", Map("Content-Type"->"application/json"), Some(json))
         val result: HttpResponse = req.exec[IO].unsafeRunSync()
        //debug(s"calling back ${cb.backURL} with request:")
        //debug(json)
        //debug("and result")
        //debug(result)
        //debug(s"deleting callback ${cb.backURL} from the list")
        r.id -> cb
      }
      val g: Map[String, Set[MessagesAPI.CallBack]] = toDelete.groupBy(_._1).mapValues(_.map(_._2).toSet)
      if(g.nonEmpty) {
        val updCallbacks = callbacks.map{ case (i, cbs) => if(g.contains(i)) (i, cbs -- g(i)) else (i, cbs)}.filter(_._2.nonEmpty)
        //debug(s"deleting ${callbacks.size - updCallbacks.size} callbacks after execution!")
        context.become(operation(updCallbacks))
      }

    case mes @ MessagesAPI.ServerCommand(Commands.Run(wdl, input, options, dependencies), _, _) =>
          val source: ActorRef = sender()
          val statusUpdate = client.postWorkflowStrings(wdl, input, options, dependencies)
          statusUpdate pipeTo source
          statusUpdate.map(s=>mes.promise(s)) pipeTo self

    case Commands.GetAllMetadata(status, subworkflows) =>
      val s = sender
      client.getAllMetadata(status, subworkflows).map(m=> Results.UpdatedMetadata(m)).unsafeToFuture().pipeTo(s)

    case Commands.GetQuery(status, subs) =>
      val s = sender
      client.getQuery(status, subs).unsafeToFuture().pipeTo(s)

    case Commands.SingleWorkflow.GetOutput(id) =>
      val s = sender()
      client.getOutput(id).unsafeToFuture().pipeTo(s)

    case Commands.SingleWorkflow.GetStatus(id) =>
      val s = sender()
      client.getStatus(id).unsafeToFuture().pipeTo(s)

    case promise @ MessagesAPI.ServerPromise(status, backs) =>
      callbacks.get(status.id) match {
        case Some(cbs) => context.become(operation(callbacks.updated(status.id, cbs ++ backs)))
        case None => context.become(operation(callbacks + (status.id -> backs)))
      }
  }


  override def receive: Receive = operation(Map.empty)
}
