package group.research.aging.cromwell.web

import java.time.{Duration, OffsetDateTime, ZoneOffset}

import cats.implicits._
import group.research.aging.cromwell.client.{QueryResults, WorkflowStatus}
import group.research.aging.cromwell.web.Commands.QueryWorkflows
import group.research.aging.cromwell.web.utils.Uploader
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.Event
import dom.ext._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.HTMLInputElement
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import scala.scalajs.js
import scala.util.{Failure, Success, Try}
import scala.xml.Elem
import org.querki.jquery.$
/**
  * View for loading workflows and running workflows
  * @param commands Var to run commands
  * @param messages Var to send messages
  * @param lastURL last URL of Cromwell server
  * @param loaded metadata of the loaded workflows
  * @param heartBeat hearbeat signal to check that server is alive
  */
class RunnerView(
                  commands: Var[Commands.Command],
                  messages: Var[Messages.Message],
                  lastQuery: Rx[WorkflowQueryLike],
                  lastURL: Rx[String],
                  loaded: Rx[(Int, Int)],
                  pipelines: Rx[Pipelines],
                  heartBeat: Rx[HeartBeat])
  extends Uploader with BasicView {


  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  val counter: Var[Int] = Var(0)

  //lazy val defURL = "http://agingkills.westeurope.cloudapp.azure.com" //"http://localhost:8000"

  //def defURL: String = CromwellClient.localhost.base

  val wdlFile: Var[Option[String]] = Var(None)

  val dependencies: Var[List[(String, String)]] = Var(List.empty)

  val inputs: Var[Option[String]] = Var(None)

  val options: Var[Option[String]] = Var(None)

  val url: Var[String] = Var("") //Var("http://agingkills.westeurope.cloudapp.azure.com") //"http://localhost:8000"


  val lastLimit: Rx[Int] = lastQuery.map(_.limit)
  val lastOffset: Rx[Int] = lastQuery.map(_.offset)
  val lastStatus: Rx[WorkflowStatus] = lastQuery.map(_.status)
  val lastExpandSubworkflows: Rx[Boolean] = lastQuery.map(_.expandSubworkflows)

  //val currentStatus: Var[WorkflowStatus] = Var(WorkflowStatus.AnyStatus)

  val query: Var[Commands.QueryWorkflows] = Var(Commands.QueryWorkflows.default)

  val currentPipeline: Var[Pipeline] = Var(Pipeline.empty)

  def init() = {
    lastURL.impure.run{ u=> url := u }
    lastQuery.dropRepeats.impure.run{ q=>
      query := QueryWorkflows(q)
    }
    pipelines.impure.run{ ps=>
      currentPipeline :=  ps.pipelines.headOption.getOrElse(Pipeline.empty)
    }

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
      //limit := str.toInt
      query := query.now.copy(limit = str.toInt)
    }
  }

  protected def offsetHandler(event: Event): Unit = {
    val target = event.currentTarget//.value.asInstanceOf[String]
    val str: String = target.asInstanceOf[Input].value
    Try{
      query := query.now.copy(offset = str.toInt)
    }
  }

  protected def subworkflowsHandler(event: Event): Unit = {
    val target = event.currentTarget//.value.asInstanceOf[String]
    val str: String = target.asInstanceOf[Input].value
    Try{
      val checkbox = dom.document.getElementById("expand").asInstanceOf[HTMLInputElement]
     query := query.now.copy(expandSubworkflows = checkbox.checked)
    }
  }
  val queryResults = Var(QueryResults.empty)

  protected def updateClick(event: Event): Unit = {
    //println("URL == "+getURL())
    commands := Commands.ChangeClient(getURL())
    commands := Commands.QueryWorkflows(query.now)
  }

  protected def updateStatus(event: Event): Unit = {
    val value = dom.document.getElementById("status").asInstanceOf[dom.html.Select].value
    commands := Commands.UpdateStatus(WorkflowStatus.withName(value))
  }

  protected def selectPipeline(event: Event): Unit = {
    val value = dom.document.getElementById("pipelines").asInstanceOf[dom.html.Select].value
    commands := Commands.SelectPipeline(value)
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
    commands := query.now
  }

  protected def uploadFileHandler(v: Var[Option[String]])(event: Event): Unit = {
    uploadHandler(event){
      case Success((f, str)) => v := Some(str)
      case Failure(th)=>
        messages := Messages.ExplainedError("failed uploading files", th.getMessage)
    }
  }

  protected def uploadFilesHandler(v: Var[List[(String, String)]])(event: Event): Unit = {
    uploadMultipleHandler(event){
      case Success(seq) =>
        println("uploaded files: " + v.now.map(_._1))
        v := seq.map{ case (f, c) => f.name -> c}.toList
      case Failure(th)=>
        dom.console.error(th.getMessage)
        messages := Messages.ExplainedError("failed uploading files", th.getMessage)
    }
  }
  protected def runClick(event: js.Dynamic): Unit = if(tab.now == "manual" || currentPipeline.now == Pipeline.empty){
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
  } else {
    val p = currentPipeline.now

    commands := p.to_run(inputs.now.getOrElse(""),options.now.getOrElse(""))
  }

  protected def activeClick(tb: String)(event: dom.Event): Unit = {
    tab := tb
  }


  protected def validateClick(event: js.Dynamic): Unit = if(tab.now == "manual" || currentPipeline.now == Pipeline.empty){
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
  } else {
    commands := Commands.Validate(currentPipeline.now.main,
      inputs.now.getOrElse(""),
      options.now.getOrElse(""),
      currentPipeline.now.dependencies
    )
  }

  protected def cleaner(id: String, fun: ()=>Unit)(event: Event): Unit =  if(event.target == event.currentTarget){
    dom.document.getElementById(id) match {
      case inp: HTMLInputElement =>
        inp.value = null
        fun()
      case v => dom.console.log(s"click on ${id}"+ v.toString)
    }
  }


  def option(value: String, label: String, default: String): Elem = if(default ==value)
    <option selected="selected" value={value}>{label}</option>
  else <option value={value}>{label}</option>

  val tab = Var("manual")

  val inManualTab: Rx[Boolean] = tab.map(t=>t=="manual")
  val hasPipelines: Rx[Boolean] = pipelines.map(_.pipelines.nonEmpty)
  val inPipelinesTab: Rx[Boolean] = tab.zip(hasPipelines).map{ case (t, p)=> p && t=="pipelines" }


  val alwaysFalse = Var(false)


  val canRun: Rx[Boolean] = for{
    u <- validUrl
    i <- inputs
    m <- inManualTab
    w <- wdlFile
    p <- hasPipelines
  } yield (u && i.isDefined) && (m && w.isDefined || p)


  val topMenu: Elem = <div class="ui top big fixed menu">
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
        { query.map{ q=>
        WorkflowStatus.values.map(s =>option(s.entryName, s.entryName, q.status.entryName))
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
    <section class="item">
      <div class="ui checked checkbox">
        <input id="expand" name ="expand"  type="checkbox" checked={query.map(q=>q.expandSubworkflows.toString)} onclick = { subworkflowsHandler _}></input>
          <label>expand subworkflows</label>
        </div>
    </section>
    <section class="item">{loaded.dropRepeats.map{ case (l, total) =>
        <small>[<b>{l} of {total}</b>] loaded</small>
      }
        }</section>
    <section class="item">
      <i class={
         heartBeat.dropRepeats.map { h => h.warning match {
             case None => "big red ban icon"
             case Some(true) => "orange circle outline icon"
             case Some(false) => "green circle outline icon"
           }
         }
         }></i><small>connection</small>
    </section>
    </div>


  val bottomMenu =
    <div class="ui bottom fixed pointing menu" style="overflow-x:scroll;">
      <section class="item">
        <button class={ enabledIf("ui big primary button", canRun) } onclick = { runClick _}>Run</button>
      </section>
        <section class="item">
          <button class={ enabledIf("ui primary button", canRun) } onclick = { validateClick _}>Validate</button>
        </section>
        <div class="ui stackable menu" id ="input_menu">
          <section class="item tab segment active">
            <div class="ui label">inputs json/yaml</div>
            <input id ="inputs" onchange = { uploadFileHandler(inputs) _ } accept=".json,.yaml,.yml" name="inputs" type="file" >
            </input>
            <i class="remove icon" style={visibleIfDefined(inputs)} onclick={cleaner("inputs", ()=>inputs := None) _}></i>
          </section>
        </div>
        <div style={visibleIf(hasPipelines)} class={stringIfElse(inManualTab, "active blue tab item", "blue tab item")}
             data-tab="manual" onmousedown ={ activeClick("manual") _ }>
          Manual
        </div>
      <div class="ui stackable blue menu" id ="manual_menu" style={visibleIf(inManualTab)}>
        <section class="item tab segment active"  data-tab="manual">
          <div class="ui label">workflow WDL or CWL</div>
          <input id ="wdl" onchange = { uploadFileHandler(wdlFile) _ } accept=".wdl,.cwl"  name="wdl" type="file" >
          </input>
          <i class="remove icon" style={visibleIfDefined(wdlFile)} onclick={cleaner("wdl", ()=>wdlFile := None) _}></i>
        </section>
        <section class="item tab segment active" data-tab="manual">
          <div class="ui label">dependencies</div>
          <input id ="dependencies"
                 onchange = { uploadFilesHandler(dependencies) _ }
                 accept=".wdl,.cwl"  name="dependencies" type="file" multiple="multiple" >
            </input>
          <i class="remove icon" style={visibleIfHasElements(dependencies)} onclick={cleaner("dependencies", ()=> dependencies := List.empty) _}></i>
        </section>
        <section class="item tab segment active" data-tab="manual">
          <div class="ui label">options</div>
          <input id ="options" onchange = { uploadFileHandler(options) _ } accept=".json,.yaml,.yml" name="options" type="file" >
          </input>
          <i class="remove icon"  style={visibleIfDefined(options)} onclick={cleaner("options", ()=>options := None) _}></i>
        </section>
      </div>
      <div style={visibleIf(hasPipelines)}
           class={stringIfElse(inPipelinesTab, "active blue tab item", "blue tab item")} data-tab="pipelines"
           onmousedown ={ activeClick("pipelines") _ }>
        Pipelines
      </div>
      <div class="ui stackable blue menu" id ="pipelines_menu" style={visibleIf(inPipelinesTab)}>
        <section class="item tab segment" data-tab="pipelines">
          <select id="pipelines" onclick={selectPipeline _}>
            { pipelines.map{ ps=> ps.pipelines.map{p =>
                option(p.name, p.name, ps.pipelines.headOption.map(_.name).getOrElse(""))
                }
              }
            }
          </select>
        </section>
      </div>

    </div>
  val rightMenu =
    <section class="ui right fixed vertical blue menu" style={visibleIf(alwaysFalse)}>
      <div class="item">
        WDL:
        <div contenteditable="true">{wdlFile}</div>
      </div>
      <div class="item">
        Inputs:
        <div contenteditable="true">{inputs}</div>
      </div>
    </section>


  init()


}