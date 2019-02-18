package group.research.aging.cromwell.web

import group.research.aging.cromwell.client.CromwellClient
import group.research.aging.cromwell.web.Messages.ExplainedError
import group.research.aging.cromwell.web.communication.{WebsocketClient, WebsocketMessages}
import org.querki.jquery._
import wvlet.log.LogLevel

import scala.util.Random

/**
  * Main application
  * It based on a Redux pattern: all the state is saved in a State veraible while different actions are used to change the state
  * Uses MonadicHTML libraries (similar to ScalaRx) for DataBinding
  */
object CromwellWeb extends scala.App with Base {

  logger.setLogLevel(LogLevel.DEBUG)

  import mhtml._
  import org.scalajs.dom

  lazy val table: JQuery = $("workflows")

  /**
    * commands, messages and results are main types of actions, they are put inside Var-s for DataBinding purposes
    * Use := to update the Var
    */
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
      Results.ServerResult(ExplainedError("unexpected message from websocket!", other.toString))

  }(Results.ServerResult(EmptyAction))

  val allActions: Var[Action] = Var(EmptyAction)

  protected def uglyUpdate(rxes: Rx[Action]*) = {
    for(r <- rxes) r.impure.run(v=> allActions := v)
  }

  /**
    * Contains the state of the application, wrapped into Var for databinding purposes
    */
  val state: Var[State] = Var(State.empty)

  /**
    * All changes of the state happens in onReduce that is a partial function that is composed (for the sake of convenience) from several Reducers
    */
  lazy val commandsReducer: Reducer = {

    case (previous, getMetadata: Commands.GetAllMetadata) =>
      commands := Commands.SendToServer(getMetadata)
      previous

    case (previous, Commands.ChangeClient(url)) =>
      if (previous.client.base != url){
        dom.window.localStorage.setItem(Commands.LoadLastUrl.key, url)
        previous.copy(client = CromwellClient(url)).withEffect{() =>
          commands := Commands.SendToServer(Commands.ChangeClient(url))
          //commands := Commands.GetMetadata(state.now.status)
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

    case (previous, a @ Commands.Abort(id)) =>
      toServer := WebsocketMessages.WebsocketAction(a)
      previous

    case (previous, run @ Commands.Run(wdl, input, options, dependencies)) =>
      //debug("DEPENDENCIES: \n"+ dependencies.mkString("\n"))
      previous.withEffect{() =>
        toServer := WebsocketMessages.WebsocketAction(run)
      }

    case (previous, v @ Commands.Validate(wdl, input, options, dependencies)) =>
      //debug("DEPENDENCIES: \n"+ dependencies.mkString("\n"))
      previous.withEffect{() =>
        toServer := WebsocketMessages.WebsocketAction(v)
      }
  }

  lazy val resultsReducer: Reducer = {

    case (previous, Results.ServerResult(action)) =>
      previous.withEffect{() =>
        action match {
          case c: Commands.Command => commands := c
          case r: Results.ActionResult => results := r
          case m: Messages.Message =>  messages := m
          case EmptyAction | KeepAliveAction =>
          case other =>  error(s"Unknown server message: \n ${other}")
        }
      }

    case (previous, Results.UpdatedStatus(md)) =>
      println(s"not yet sure what to do with updated status: ${md}")
      previous

    case (previous, Results.WorkflowValidated(ers)) =>
      if(ers.errors.nonEmpty)
      {
        messages := Messages.Errors(ers.errors.map(ExplainedError("workflow validation error", _)))
      }
      else {
        messages := Messages.Infos(List(Messages.Info("validation result", "workflow validated successfully!")))
      }
      previous


    case (previous, Results.UpdateClient(base)) => previous.copy(client = previous.client.copy(base = base), errors = Nil, infos = Nil)
    case (previous, upd: Results.UpdatedMetadata) => previous.copy(metadata = upd.metadata, errors = Nil, infos = Nil)

  }

  //AppCircuit.addProcessor(new LoggingProcessor[AppModel]())
  val runner = new RunnerView(commands, messages, state.map(_.status), state.map(_.client.base))


  val workflows = new WorkflowsView(
    state.map(_.sortedMetadata),
    state.map(_.client.baseHost),
    commands
  )

  val errors = new ErrorsView(state.map(s=>s.errors),messages)
  val infos = new InfoView(state.map(s=>s.infos), messages)


  lazy val errorReducer: Reducer = {

    case (previous, Messages.Errors(err)) =>
      error(err)
      previous.copy(errors = err)

    case (previous, e: Messages.ExplainedError) =>
      error(e)
      previous.copy(errors = e::previous.errors)

    case (previous, Messages.Infos(inf)) =>
      info(inf)
      previous.copy(infos = inf)

    case (previous, i: Messages.Info) =>
      info(i)
      previous.copy(infos = i::previous.infos)
  }

  def onOther : Reducer = {

    case (previous, action: EmptyAction) =>
      info("empty actions did not changed anything")
      previous

    case (previous, action) =>
      error(s"no state change for ${action} with ${previous}")
      previous
  }


  /**
    * Changes of the state happens only in the Reducers
    */
  lazy val onReduce: Reducer = commandsReducer
    .orElse(resultsReducer)
    .orElse(errorReducer)
    .orElse(onOther)

  val component =
  <div id="cromwell">
      {  runner.component }
      {  errors.component }
      {  infos.component }
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

  /**
    * Mounts everything together, start data binding of the HTML part
    * @return
    */
  def activate() = {
    //uglyUpdate(commands, messages, results)
    uglyUpdate(commands, messages, results, fromServer)
    //workaround to avoid foldp issues
    allActions.impure.run(onAction)
    mount(div, component)

  }

  activate()

}
