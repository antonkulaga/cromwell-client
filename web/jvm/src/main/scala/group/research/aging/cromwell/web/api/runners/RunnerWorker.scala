package group.research.aging.cromwell.web.api.runners

import akka.stream.ActorMaterializer
import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.HttpExt
import group.research.aging.cromwell.client.CromwellClientAkka
import group.research.aging.cromwell.web.{Commands, Results}
import wvlet.log.LogSupport
import akka.pattern._
import hammock.{ContentType, Hammock, Method}
import cats.effect.IO
import group.research.aging.cromwell.web.server.WebServer.http
import io.circe.generic.auto._
import hammock._
import hammock.akka.AkkaInterpreter
import hammock.marshalling._
import hammock.circe.implicits._
import hammock.hi.Opts
import io.circe.Json
import io.circe._
import io.circe.parser._
import hammock.circe._
import io.circe.syntax._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
class RunnerWorker(client: CromwellClientAkka) extends Actor with LogSupport {

  implicit def dispatcher = context.dispatcher

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
      for{
        r <- results
        if callbacks.contains(r.id)
        url <- callbacks(r.id)
      }
      {
         val json: Json = results.asJson
         val req = Hammock.request[Json](Method.POST, uri"${url.backURL}", Map("Content-Type"->ContentType.`application/json`.name), Some(json))
         val result = req.exec[IO].unsafeRunSync()
        debug(s"calling back $url with result:\n ${result}")
      }

    case mes @ MessagesAPI.ServerCommand(Commands.Run(wdl, input, options), _, _) =>
          val source: ActorRef = sender()
          val statusUpdate = client.postWorkflowStrings(wdl, input, options)
          statusUpdate pipeTo source
          statusUpdate.map(s=>mes.promise(s)) pipeTo self

    case Commands.GetAllMetadata(status, subworkflows) =>
      val s = sender
      client.getAllMetadata(status, subworkflows).map(m=> Results.UpdatedMetadata(m)).unsafeToFuture().pipeTo(s)

    case Commands.SingleWorkflow.GetOutput(id) =>
      val s = sender()
      client.getOutputs(id).unsafeToFuture().pipeTo(s)

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
