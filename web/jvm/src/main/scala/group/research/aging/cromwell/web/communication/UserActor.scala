package group.research.aging.cromwell.web.communication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorRef
import akka.pattern.pipe
import better.files.File
import cats.implicits._
import group.research.aging.cromwell.client.{CromwellClientAkka, WorkflowStatus}
import group.research.aging.cromwell.web.Commands.{ChangeClient, StreamMetadata}
import group.research.aging.cromwell.web.Results.QueryWorkflowResults
import group.research.aging.cromwell.web.common.BasicActor
import group.research.aging.cromwell.web.util.PipelinesExtractor
import group.research.aging.cromwell.web.{Commands, EmptyAction, Messages, Results}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogRotationHandler, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Actors that proccesses most of websocket messages from the users and back
  * @param username
  */
case class UserActor(username: String, initialClient: CromwellClientAkka) extends BasicActor with PipelinesExtractor {

  // Set the default log formatter
  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  lazy val heartBeatInterval = 15 seconds

  debug(s"user actor ${username}")

  val startTime: LocalDateTime = LocalDateTime.now(); // gets the current date and time

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm")

  lazy val logDir: String = Option(System.getenv("CROMWELL_LOGS")).filter(File(_).exists).getOrElse("logs")
  logger.addHandler(new LogRotationHandler(
    fileName = logDir + s"/cromwell-client_${username}_started_${startTime.format(formatter)}.log",
    maxNumberOfFiles = 100, // rotate up to 100 log files
    maxSizeInBytes = 100 * 1024 * 1024, // 100MB
    SourceCodeLogFormatter // Any log formatter you like
  ))
  debug(s"writing log to cromwell-client_${username}_started_${startTime.format(formatter)}")


  implicit def executionContext: ExecutionContextExecutor = this.context.dispatcher


  /**
    * Key functions that generates Recieve function based on outputs and server client
    * @param output
    * @param client
    * @return
    */
  protected def operation(output: List[ActorRef], client: CromwellClientAkka): Receive = {

    case WebsocketMessages.ConnectWsHandle(ref) =>
      this.context.become(operation(output :+ ref, client))
      ref ! WebsocketMessages.WebsocketAction(Results.UpdateClient(client.base))
      //info("PIPELINES ROOT: "+ this.pipelinesRoot)
      //info("SENDING PIPELINES: " + this.allPipelines)
      ref ! WebsocketMessages.WebsocketAction(Results.UpdatePipelines(this.allPipelines))

    case WebsocketMessages.WsHandleDropped =>
      info("Websocket HANDLER DROP!")

    case WebsocketMessages.WebsocketAction(action) =>
      //debug(s"WebsocketAction: \n ${action}")
      self ! action

    case q @ Commands.QueryWorkflows(status, expandSubworkflows, limit, offset) =>
      debug("=====Commands.QueryWorkflows=======")
      debug(q)
      //val metaFut: Future[Results.UpdatedMetadata] = client.getAllMetadata(status, subworkflows).map(m=> Results.UpdatedMetadata(m)).unsafeToFuture()
      val queryResults = client.getQuery(status, expandSubworkflows)
        .map(r=>QueryWorkflowResults(r, Map.empty, status, expandSubworkflows, limit, offset).paginate(limit, offset)).unsafeToFuture()
      pipe(queryResults)(context.dispatcher) to self

    case r: Results.QueryWorkflowResults =>
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(r))
      for(id <- r.missing) {
        val metaFut = client.getMetadata(id).map(r=>Results.UpdatedMetadata(Map(r.id-> r))).unsafeToFuture()
        pipe(metaFut)(context.dispatcher) to self
      }

    case r: Results.UpdatedMetadata =>
      output.foreach(o=> o ! WebsocketMessages.WebsocketAction(r))

    case Commands.CleanMessages =>
      this.context.become(operation(output, client))

    case Commands.Validate(wdl, input, options, dependencies) =>
      info(s"VALIDATING WDL at ${client.base}")
      debug("DEPENDENCIES: " + dependencies.map(_._1))
      //debug(wdl)
      debug("-------------")
      debug("INPUT: ")
      debug(input)
      debug("=================================")
      val postFut = client.validateWorkflow(wdl, input, options, dependencies).map(v => Results.WorkflowValidated(v)).recover{
        case th =>
          error(s"WORKFLOW could not be executed because of: \n ${th}")
          val m = Option(th.getMessage).combine(Option(th.getCause).map(_.getMessage)).getOrElse(th.toString)
          Messages.Errors(List(Messages.ExplainedError("workflow could not be executed!", m)))
      }
      pipe(postFut)(context.dispatcher) to self


    case v: Results.WorkflowValidated =>
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(v))


    case Commands.Run(wdl, input, options, dependencies) =>
      info(s"RUNNING WDL at ${client.base}")
      //debug(wdl)
      debug("-------------")
      debug("INPUT: ")
      debug(input)
      debug("=================================")
      val postFut = client.postWorkflowStrings(wdl, input.replace("\t", "  "), options, dependencies)
        .map[Results.ActionResult](s=>Results.WorkflowSent(s))(context.dispatcher).recover{
        case th =>
          error(s"WORKFLOW could not be executed because of: \n ${th}")
          val m = Option(th.getMessage).combine(Option(th.getCause).map(_.getMessage)).getOrElse(th.toString)
          Messages.Errors(List(Messages.ExplainedError("workflow could not be executed!", m)))
      }
      pipe(postFut)(context.dispatcher) to self

    case Results.WorkflowSent(status) =>
      context.system.scheduler.scheduleOnce(3 seconds, self, Commands.QueryWorkflows(WorkflowStatus.AnyStatus, true))

    case ChangeClient(newURL) =>
      debug(s"CHANGE CLIENT to ${newURL}!")
      val newClient = client.copy(base = newURL)(initialClient.http, initialClient.materializer)
      this.context.become(operation(output, newClient))

    case Commands.Abort(id) =>
      debug(s"aborting ${id}")
      val abortion =  client.abort(id)
        .map(s=>Messages.Infos(List(Messages.Info("abortion", s"aborting ${s.id}, current status: ${s.status}")))).unsafeToFuture()
        //.map(_=>Commands.QueryWorkflows(WorkflowStatus.AnyStatus))
      pipe(abortion)(context.system.dispatcher).pipeTo(self)
      //val update = akka.pattern.after(3 seconds, context.system.scheduler)(abortion.map(_ => Commands.QueryWorkflows(WorkflowStatus.AnyStatus, true)))(context.system.dispatcher)
      //pipe(update)(context.system.dispatcher).pipeTo(self)


    case s : Messages.Infos =>
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(s))

    case e: Messages.Errors =>
      output.foreach(o=>o ! WebsocketMessages.WebsocketAction(e))

    case akka.actor.Status.Failure(th) =>
      val er =  Messages.Errors(Messages.ExplainedError(s"running workflow at ${client.base} failed", Option(th.getMessage).getOrElse(""))::Nil)
      debug(er)
      self !  er


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

  override def receive: Receive = operation(Nil, initialClient)

}