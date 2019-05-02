package group.research.aging.cromwell.web

import java.time.{Duration, OffsetDateTime, ZoneOffset}

import cats.implicits._
import group.research.aging.cromwell.client.{QueryResults, WorkflowStatus}
import group.research.aging.cromwell.web.utils.Uploader
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input

import scala.scalajs.js
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

/**
  * View for loading workflows and running workflows
  * @param commands Var to run commands
  * @param messages Var to send messages
  * @param currentStatus
  * @param lastURL last URL of Cromwell server
  * @param lastLimit limit workflows that will be used
  * @param lastOffset offset for the workflows
  * @param loaded metadata of the loaded workflows
  * @param heartBeat hearbeat signal to check that server is alive
  */
class RunnerView(
                  commands: Var[Commands.Command],
                  messages: Var[Messages.Message],
                  currentStatus: Rx[WorkflowStatus],
                  lastURL: Rx[String],
                  lastLimit: Rx[Int],
                  lastOffset: Rx[Int],
                  loaded: Rx[(Int, Int)],
                  heartBeat: Rx[HeartBeat])
  extends Uploader{


  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  val counter = Var(0)

  //lazy val defURL = "http://agingkills.westeurope.cloudapp.azure.com" //"http://localhost:8000"

  //def defURL: String = CromwellClient.localhost.base

  val wdlFile: Var[Option[String]] = Var(None)

  val dependencies: Var[List[(String, String)]] = Var(List.empty)

  val inputs: Var[Option[String]] = Var(None)

  val options: Var[Option[String]] = Var(None)

  val url = Var("") //Var("http://agingkills.westeurope.cloudapp.azure.com") //"http://localhost:8000"

  val limit = Var(25)
  val offset = Var(0)

  val lastStatus: Var[WorkflowStatus] = Var(WorkflowStatus.AnyStatus)

  def init() = {
    lastURL.impure.run{ u=>
      url := u
    }
    currentStatus.impure.run{ s=>
      lastStatus := s
    }

    lastLimit.dropRepeats.impure.run(l=> limit:= l)
    lastOffset.dropRepeats.impure.run(o=> offset := o)

  }

  def getURL(): String = url.now


  lazy val proxy: String = {
    val protocol = dom.window.location.protocol
    val host = dom.window.location.host
    protocol + "//" + host + "/"
  }

  def hasHost: Rx[Boolean] = url.map(_.contains(proxy))

  val validUrl: Rx[Boolean] = url.map(u=>u.contains("http") && u.contains("://"))

  val validUpload: Rx[Boolean] = for{
    u <- validUrl
    w <- wdlFile
    i <- inputs
  } yield  w.isDefined && i.isDefined && u


  val autoUpdate = Var(0)

  //currenUrl.impure.run(v=>url := v)


  protected def updateHandler(event: Event): Unit = {
    val target = event.currentTarget//.value.asInstanceOf[String]
    val str: String = target.asInstanceOf[Input].value
    url := str
  }

  protected def limitHandler(event: Event): Unit = {
    val target = event.currentTarget//.value.asInstanceOf[String]
    val str: String = target.asInstanceOf[Input].value
    Try{
      limit := str.toInt
    }
  }

  protected def offsetHandler(event: Event): Unit = {
    val target = event.currentTarget//.value.asInstanceOf[String]
    val str: String = target.asInstanceOf[Input].value
    Try{
      offset := str.toInt
    }
  }

  val queryResults = Var(QueryResults.empty)

  protected def updateClick(event: Event): Unit = {
    //println("URL == "+getURL())
    commands := Commands.ChangeClient(getURL())
    commands := Commands.QueryWorkflows(lastStatus.now, offset = offset.now, limit = limit.now)
  }

  protected def updateStatus(event: Event): Unit = {
    val value = dom.document.getElementById("status").asInstanceOf[dom.html.Select].value
    commands := Commands.UpdateStatus(WorkflowStatus.withName(value))
  }

  /*
  protected def proxyClick(event: Event): Unit = {
    val u: String = url.now
    if(u.contains(proxy))
      messages := Messages.ExplainedError(s"Proxy button should not be visible if url contains host. Current URL is ${u}, host is ${proxy}" , "")
    else {
      val newValue = proxy  + u
      url := newValue
      commands:= Commands.ChangeClient(newValue)
      //commands := Commands.GetMetadata
    }
  }
  */

  protected def localhostClick(event: Event): Unit = {
    val d = "http://localhost:8000"
    //url := d
    commands:= Commands.ChangeClient(d)
    commands := Commands.QueryWorkflows(lastStatus.now)
  }

  protected def uploadFileHandler(v: Var[Option[String]])(event: Event): Unit = {
    uploadHandler(event){
      case Success((f, str)) => v := Some(str)
      case Failure(th)=> dom.console.error(th.getMessage)
    }
  }

  protected def uploadFilesHandler(v: Var[List[(String, String)]])(event: Event): Unit = {
    uploadMultipleHandler(event){
      case Success(seq) => v := seq.map{ case (f, c) => f.name -> c}.toList
      case Failure(th)=>
        dom.console.error(th.getMessage)
        messages := Messages.ExplainedError("failed uploading files", th.getMessage)
    }
  }
  protected def runClick(event: js.Dynamic): Unit = {
    wdlFile.now match {
      case Some(wdl: String) =>
        val toRun = Commands.Run(wdl,
          inputs.now.getOrElse(""),
          options.now.getOrElse(""),
          dependencies.now
        )
        commands := toRun
      case None => messages := Messages.ExplainedError("No WLD file uploaded!" ,"")
    }
  }

  protected def validateClick(event: js.Dynamic): Unit = {
    wdlFile.now match {
      case Some(wdl: String) =>
        val validate = Commands.Validate(wdl,
          inputs.now.getOrElse(""),
          options.now.getOrElse(""),
          dependencies.now
        )
        commands := validate
      case None => messages := Messages.ExplainedError("No WLD file uploaded!" ,"")
    }
  }

  def enabledIf(str: String, condition: Rx[Boolean]): Rx[String] =
    condition.map(u=>
      if (u) {
        str
      } else s"$str disabled"
    )


  def option(value: String, label: String, default: String): Elem = if(default ==value)
    <option selected="selected" value={value}>{label}</option>
  else <option value={value}>{label}</option>

  val menu: Elem = <div class="ui top big fixed menu">
      <section class="item">
        <div class={enabledIf("ui big primary button", validUrl)} onclick={ updateClick _}>Update workflows</div>

      </section>
      <section class="item">
        {lastURL.dropRepeats.map{ u =>
            <input id="url" type="text" placeholder="Enter cromwell URL..."  oninput={ updateHandler _ } value={ u } />
        }}
      </section>
    <section class="item">
      <select id="status"  onclick={ updateStatus _}>
        { currentStatus.map{ status=>
        WorkflowStatus.values.map(s =>option(s.entryName, s.entryName, status.entryName))
      }  }
      </select>
      <datalist id="status" value={WorkflowStatus.AnyStatus.entryName}></datalist>
    </section>
      <section class="item">
        {lastLimit.dropRepeats.map{ u =>
          <input id="url" type="number" min="0" max="1000" style="max-width: 50px;" placeholder="LIMIT"  oninput={ limitHandler _ } value={ u.toString } />
      }}
        {lastOffset.dropRepeats.map{ u =>
          <input id="url" type="number" min="0" max="100000" style="max-width: 50px;" placeholder="OFFSET"  oninput={ offsetHandler _ } value={ u.toString } />
      }}
      </section>
    <section class="item">{loaded.dropRepeats.map{ case (l, total) =>
        <small>[<b>{l} of {total}</b>] loaded</small>
      }
        }</section>
    <section class="item">
      <i class={
         heartBeat.dropRepeats.map {
           case h => h.warning match {

             case None =>
               "big red ban icon"
             case Some(true) => "orange circle outline icon"
             case Some(false) => "green circle outline icon"
           }
         }
         }></i><small>connection</small>
    </section>
    </div>

  val component: Elem = <div class="ui bottom fixed menu">
    <section class="item">
      <button class={ enabledIf("ui big primary button", validUpload) } onclick = { runClick _}>Run</button>
    </section>
    <section class="item">
      <button class={ enabledIf("ui primary button", validUpload) } onclick = { validateClick _}>Validate</button>
    </section>
    <section class="item">
        <div class="ui label">workflow WDL</div>
        <input id ="wdl" onclick="this.value=null;" onchange = { uploadFileHandler(wdlFile) _ } accept=".wdl"  name="wdl" type="file" />
    </section>
    <section class="item">
        <div class="ui label">dependencies</div>
        <input id ="wdl" onclick="this.value=null;" onchange = { uploadFilesHandler(dependencies) _ } accept=".wdl"  name="dependencies" type="file" multiple="multiple" />
    </section>
    <section class="item">
        <div class="ui label">inputs json</div>
        <input id ="inputs" onclick="this.value=null;" onchange = { uploadFileHandler(inputs) _ } accept=".json" name="inputs" type="file" />
    </section>
    <section class="item">
        <div class="ui label">options</div>
        <input id ="inputs" onclick="this.value=null;" onchange = { uploadFileHandler(inputs) _ } accept=".json" name="options" type="file" />
    </section>
  </div>


  init()


}