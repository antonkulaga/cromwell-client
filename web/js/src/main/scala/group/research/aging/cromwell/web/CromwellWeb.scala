package group.research.aging.cromwell.web

import group.research.aging.cromwell.client.CromwellClient
import group.research.aging.cromwell.web.communication.{WebsocketClient, WebsocketMessages}
import org.querki.jquery._
import wvlet.log.LogLevel

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Random, Success}

/**
  * Main application
  */
object CromwellWeb extends scala.App with Base {

  logger.setLogLevel(LogLevel.DEBUG)

  import mhtml._
  import org.scalajs.dom

  lazy val table: JQuery = $("workflows")


  val commands: Var[Commands.Command] = Var(Commands.EmptyCommand)
  val messages: Var[Messages.Message] = Var(Messages.EmptyMessage)
  val results: Var[Results.ActionResult] = Var(Results.EmptyResult)


  protected lazy val randomUser = "user" + Random.nextInt(10000)
  val websocketClient: WebsocketClient = WebsocketClient.fromRelativeURL("ws" + "/" + randomUser)
  val toServer: Var[WebsocketMessages.WebsocketMessage] = websocketClient.toSend

  val fromServer: Rx[Results.ServerResult] = websocketClient.messages.collect{
    case WebsocketMessages.WebsocketAction(a) =>
      Results.ServerResult(a)

    case other=>
      error("unexpected message from websocket!")
      error(other)
      Results.ServerResult(EmptyAction)

  }(Results.ServerResult(EmptyAction))

  val allActions: Var[Action] = Var(EmptyAction)

  protected def uglyUpdate(rxes: Rx[Action]*) = {
    for(r <- rxes) r.impure.run(v=> allActions := v)
  }


  val state: Var[State] = Var(State.empty)


  lazy val commandsReducer: Reducer = {

    case (previous, getMetadata: Commands.GetMetadata) =>
      commands := Commands.SendToServer(getMetadata)
      previous

    case (previous, Commands.ChangeClient(url)) =>
      if (previous.client.base != url){
        dom.window.localStorage.setItem(Commands.LoadLastUrl.key, url)
        previous.copy(client = CromwellClient(url)).withEffect{() =>
          commands := Commands.SendToServer(Commands.ChangeClient(url))
          commands := Commands.GetMetadata(state.now.status)
        }
      } else previous

    case (previous, Commands.SendToServer(action)) =>
      previous.withEffect{() =>
        toServer := WebsocketMessages.WebsocketAction(action)
      }

    case (previous, Commands.LoadLastUrl) =>
      Option(dom.window.localStorage.getItem(Commands.LoadLastUrl.key)).fold(
        previous)(url=>  previous.copy(client = CromwellClient(url)))

    case (previos, Commands.UpdateStatus(status)) => previos.copy(status =  status)


    case (previous, Commands.EvalJS(code)) =>
      scalajs.js.eval(code)
      previous

    case (previous, run @ Commands.Run(wdl, input, options)) =>
      previous.withEffect{() =>
        toServer := WebsocketMessages.WebsocketAction(run)
        val fut = previous.client.postWorkflowStrings(wdl, input, options)
        fut.onComplete{
          case Success(upd) =>
            commands := Commands.GetMetadata(state.now.status)
          case Failure(th) =>
            messages := Messages.Errors(Messages.ExplainedError(s"running workflow at ${previous.client.base} failed", Option(th.getMessage).getOrElse(""))::Nil)
        }
      }
  }

  lazy val resultsReducer: Reducer = {

    case (previos, Results.ServerResult(action)) =>
      previos.withEffect{() =>
        action match {
          case c: Commands.Command => commands := c
          case r: Results.ActionResult => results := r
          case m: Messages.Message =>  messages := m
          case other =>  error(s"Unkwon server message: \n ${other}")
        }
      }

    case (previous, Results.UpdatedStatus(md)) =>
      println("not yet sure what to do with updated status")
      previous

    case (previous, upd: Results.UpdatedMetadata) => previous.copy(metadata = upd.metadata, errors = Nil)

  }

  //AppCircuit.addProcessor(new LoggingProcessor[AppModel]())
  val runner = new RunnerView(commands, messages, state.map(_.status), state.map(_.client.base))
  val workflows = new WorkflowsView(
    state.map(_.sortedMetadata),
    state.map(_.client.base)
  )

  val errors = new ErrorsView(state.map(_.errors), messages)


  lazy val errorReducer: Reducer = {

    case (previous, Messages.Errors(err)) =>
      error(err)
      previous.copy(errors = err)

    case (previous, e: Messages.ExplainedError) =>
      error(e)
      previous.copy(errors = e::previous.errors)
  }

  def onOther : Reducer = {

    case (previous, action: EmptyAction) =>
      info("empty actions did not changed anything")
      previous

    case (previous, action) =>
      error(s"no state change for ${action} with ${previous}")
      previous
  }


  lazy val onReduce: Reducer = commandsReducer
    .orElse(resultsReducer)
    .orElse(errorReducer)
    .orElse(onOther)

  val component =
  <div id="cromwell">
    <section class="ui grid">
          {  runner.component }
      {  errors.component }
    </section>
    {  workflows.component }
  </div>

  // Compute the new state given an action and a previous state:
  // (I'm really not convinced by the name)
  def reducer(previousState: State, action: Action): State = {
    debug(action)
    pprint.PPrinter.BlackWhite.pprintln(action)
    println("---------------------------------")
    onReduce(previousState, action)
  }


  protected def onAction(action: Action): Unit = {
    val currentState: State = state.now
    val newState = reducer(currentState, action)
    if(newState != currentState) {
      val effects = newState.effects
      state := newState.copy(effects  = Nil)
      effects.foreach(e=> e()) //ugly workaround for effects
    }
  }

  val div = dom.document.getElementById("main")

  def activate() = {
    //uglyUpdate(commands, messages, results)
    uglyUpdate(commands, messages, results, fromServer)
    //workaround to avoid foldp issues
    allActions.impure.run(onAction)
    mount(div, component)

  }

  activate()

}
