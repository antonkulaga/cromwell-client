package group.research.aging.cromwell.web.api.runners

import akka.actor._
import akka.pattern._
import akka.stream._
import group.research.aging.cromwell.client.{CromwellClient, QueryResult, QueryResults, StatusAndOutputs, StatusInfo}
import group.research.aging.cromwell.web.Commands.TestRun
import group.research.aging.cromwell.web.api.runners.MessagesAPI.CallBack
import group.research.aging.cromwell.web.common.BasicActor
import group.research.aging.cromwell.web.{Commands, Results}
import io.circe.Json
import io.circe.syntax._
import sttp.client3._
import sttp.model.{Header, Uri}
import sttp.client3.circe._
import io.circe.generic.auto._

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try


/**
  * Actors that interacts with cromwell server, receives messages from API and returns stuff back
  * @param client
  */
class RunnerWorker(client: CromwellClient) extends BasicActor {

  debug(s"runner worker for ${client.base} cromwell server started!")

  import scala.concurrent.duration._

  val decider: Supervision.Decider = {
    case _: ArithmeticException ⇒ Supervision.Resume
    case _                      ⇒ Supervision.Restart
  }

  implicit def dispatcher: ExecutionContextExecutor = context.dispatcher

  context.system.scheduler.schedule(
    2 second,
    8 seconds,
    self,
    MessagesAPI.Poll)

  implicit val materializer: ActorMaterializer = ActorMaterializer( ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  /**
    * Recieve generating functions
    * @param callbacks callbacks to check for responses from servers
    * @return new Receive function
    */
  protected def operation(callbacks: Map[String, Set[MessagesAPI.CallBack]]): Receive = {


    case MessagesAPI.Poll => //checks running workflows and fires callbacks and batches
      //debug(s"polling the callbacks, currently ${callbacks.mkString(",")} are avaliable")
      //val queryResults: QueryResults = client.getQuery().unsafeRunSync()
      val queryResults = client.runtime.unsafeRunTask(client.getQueryZIO())
      /*
      val finished= queryResults.filter(s=>
        s.status ==  WorkflowStatus.Succeded.entryName ||
          s.status ==WorkflowStatus.WorkflowStatus.Failed.entryName).map(_.id)
      */
      context.parent ! MessagesAPI.ServerResults(client.base, queryResults)


      val toDelete: scala.Seq[(String, MessagesAPI.CallBack)] = runForDeletion(callbacks, queryResults.results)
      val g: Map[String, Set[CallBack]] = toDelete.groupBy(_._1).map{ case (g, v) => g-> v.map(_._2).toSet}
      if(g.nonEmpty || callbacks.contains(TestRun.id)) {
        if(callbacks.contains(TestRun.id)) testResponse(callbacks)
        val updCallbacks = callbacks.filter(c => c._1 != TestRun.id)
          .map{ case (i, cbs) => if(g.contains(i)) (i, cbs -- g(i)) else (i, cbs.filter(_!=CallBack.empty))}
          .filter(_._2.nonEmpty)

        //debug(s"deleting ${callbacks.size - updCallbacks.size} callbacks after execution!")
        context.become(operation(updCallbacks))
      }


    case mes @ MessagesAPI.ServerCommand(Commands.Run(wdl, input, options, dependencies), serverURL, _, _, _, _) =>
          val source: ActorRef = sender()
          val serv = if(serverURL.endsWith("/")) serverURL.dropRight(1) else serverURL
          val cl: CromwellClient = if(client.base.contains(serv)) client else client.copy(base = serv)
          val statusUpdate = cl.postWorkflowStrings(wdl, input.replace("\t", "  "), options, dependencies)
          val statusUpdateFut = cl.runtime.unsafeRunToFuture(statusUpdate)
          statusUpdateFut pipeTo source
          statusUpdateFut.map(s=>mes.promise(s)) pipeTo self

    case mes @ MessagesAPI.ServerCommand(Commands.TestRun(wdl, input, results, dependencies), serverURL, _, _, _, _) =>
      //test server command
      val source: ActorRef = sender()
      val statusUpdate: Future[StatusInfo] = Future{
        StatusInfo("e442e52a-9de1-47f0-8b4f-e6e565008cf1-TEST", "Submitted")
      }
      statusUpdate pipeTo source
      statusUpdate.map { s =>
        val p = mes.promise(s)
        //debug(s"prossesing TEST RUN with ${Set(results)}")
        p.copy(callbacks = p.callbacks.map(v => v.copy(updateOn = Set(results))))
      } pipeTo self


    case Commands.QueryWorkflows(status, subworkflows, _, _) => //here we do not have pagination at the moment
      val s = sender
      val allmeta = client.getAllMetadataZIO(status, subworkflows).map(m=> Results.UpdatedMetadata(m.map(r=>r.id->r).toMap))
      val allmetaFut = client.runtime.unsafeRunToFuture(allmeta).future
      allmetaFut.pipeTo(s)

    case Commands.GetQuery(status, subs) =>
      val s = sender
      val query = client.getQueryZIO(status, subs)
      val queryFut = client.runtime.unsafeRunToFuture(query).future
      queryFut.pipeTo(s)

    case Commands.SingleWorkflow.GetOutput(id) =>
      val s = sender()
      val st = client.getStatusZIO(id)
      val stFut = client.runtime.unsafeRunToFuture(st).future
      val o = client.getOutputZIO(id)
      val oFut = client.runtime.unsafeRunToFuture(o).future
      val r: Future[StatusAndOutputs] = for{
        stat <-stFut
        out <- oFut
      } yield StatusAndOutputs(stat, out)
      r.pipeTo(s)

    case Commands.SingleWorkflow.GetStatus(id) =>
      val s = sender()
      val st = client.getStatusZIO(id)
      val stFut = client.runtime.unsafeRunToFuture(st).future
      stFut.pipeTo(s)

    case promise @ MessagesAPI.ServerPromise(status, backs) =>
      callbacks.get(status.id) match {
        case Some(cbs) => context.become(operation(callbacks.updated(status.id, cbs ++ backs)))
        case None => context.become(operation(callbacks + (status.id -> backs)))
      }

  }


  /**
    * Chooses callbacks that should fire and be cleaned
    * @param callbacks
    * @param results
    * @return
    */
  private def runForDeletion(callbacks: Map[String, Set[CallBack]], results: immutable.Seq[QueryResult]): Seq[(String, CallBack)] = {
    val toDelete: Seq[(String, CallBack)] = for {
      r <- results
      if callbacks.contains(r.id)
      cb: CallBack <- callbacks(r.id)
      if cb.updateOn.contains(r.status)
      outputs = client.runtime.unsafeRunTask(client.getOutputZIO(r.id))
      result = MessagesAPI.PipelineResult(r.id, r.status, outputs.outputs)
      json: Json = result.asJson
      //debug(s"sending reesults back to ${cb.backURL}")
      headers = (Map("Content-Type" -> "application/json") ++ cb.headers).map(kv=> Header(kv._1, kv._2)).toList
    }
      yield {
        debug(s"sending reesults back to ${cb.backURL}")

        //val result: HttpResponse = resultTry.get
        //val result: HttpResponse = resultTry.get
        debug(s"calling back ${cb.backURL} with request:")
        debug(json)
        debug("and result")

        //WARNING EFFECT!!!
        val resultTry = Try {
          val req = client.just_post_zio(s"${cb.backURL}", json, headers)
          client.runtime.unsafeRunTask(req)
        }
        debug(resultTry)
        debug("with headers = " + headers.mkString(";"))
        debug(s"deleting callback ${cb.backURL} from the list")
        r.id -> (resultTry match {
          case scala.util.Success(h) => cb
          case scala.util.Failure(th) =>
            error(
              s"""
                 |Failed to run ${cb.backURL} callback with the following error:
                 |${th}
              """.stripMargin)
            CallBack.empty
        })
      }
    toDelete
  }

  /**
    * Method to process only test responses
    *
    * @param callbacks
    */
  protected def testResponse(callbacks: Map[String, Set[MessagesAPI.CallBack]]): Unit = {
    for {
      cbs <- callbacks.get(TestRun.id)
      cb <- cbs
      if cb != CallBack.empty
    } {
      val headers = (Map("Content-Type" -> "application/json") ++ cb.headers).map(kv=>Header(kv._1, kv._2)).toList
      import io.circe._
      import io.circe.parser._
      val res: Option[Json] = parse(cb.updateOn.head).right.map(j => Some(j)).getOrElse(None)
      debug(s"SENDING TEST RESULT TO ${cb.backURL}:\n ${res}")
      Try{
        val post = client.just_post_zio(cb.backURL, res.get, headers)
        client.runtime.unsafeRunTask(post)
      } match {
        case  scala.util.Failure(e) => error(s"failed to callback to ${cb.backURL} with the following error ${e}")
        case scala.util.Success(result) =>debug(s"calling back ${cb.backURL} with request:")
          println("RESULT RECEIVED: " + result.toString)
          debug("with headers = " + headers.mkString(";"))
      }

    }
  }

  override def receive: Receive = operation(Map.empty)
}
