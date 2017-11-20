package group.research.aging.cromwell.web

import cats.implicits._
import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.QueryResults
import group.research.aging.cromwell.web.utils.Uploader
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.scalajs.js
import scala.util.{Failure, Success}
import scala.xml.Elem

class RunnerView(dispatcher: Dispatcher) extends Uploader{

  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  val counter = Var(0)

  lazy val defURL = "http://agingkills.westeurope.cloudapp.azure.com" //"http://localhost:8000"

  protected var url = Var(defURL)

  val wdlFile: Var[Option[String]] = Var(None)

  val inputs: Var[Option[String]] = Var(None)

  val options: Var[Option[String]] = Var(None)

  val validUrl: Rx[Boolean] = url.map(u=>u.contains("http") && u.contains("://"))

  val validUpload: Rx[Boolean] = for{
    u <- validUrl
    w <- wdlFile
    i <- inputs
  } yield  w.isDefined && i.isDefined && u


  val autoUpdate = Var(0)

  def onUrlUpdate(reader: ModelRO[String]): Unit = {
    url := reader.value
  }

  protected def updateHandler(event: js.Dynamic): Unit = {
    val str = event.target.value.asInstanceOf[String]
    url := str
  }

  val queryResults = Var(QueryResults.empty)

  protected def updateClick(event: Event): Unit = {
    //if(client.base != url.now) client = new CromwellClient("http://agingkills.westeurope.cloudapp.azure.com", "v1")
    dispatcher.dispatch(Commands.ChangeClient(url.now))
    dispatcher.dispatch(Commands.GetMetadata())
  }

  protected def localhostClick(event: Event): Unit = {
    val d = "http://localhost:8000"
    url := d
    dispatcher.dispatch(Commands.ChangeClient(d))
    dispatcher.dispatch(Commands.GetMetadata())
  }
  protected def uploadFileHandler(v: Var[Option[String]])(event: Event): Unit = {
      uploadHandler(event){
        case Success((f, str)) => v := Some(str)
        case Failure(th)=> dom.console.error(th.getMessage)
      }
  }

  protected def runClick(event: js.Dynamic): Unit = {
    val toRun = Commands.Run(
      wdlFile.now.getOrElse(""),
      inputs.now.getOrElse(""),
      options.now.getOrElse("")
    )
    dispatcher.dispatch(toRun)
  }

  def enabledIf(str: String, condition: Rx[Boolean]): Rx[String] =
    condition.map(u=>
        if (u) {
          str
        } else s"$str disabled"
    )


  val runner: Elem =
    <div class="ui menu">
      <section class="item">
        <button class={ enabledIf("ui primary button", validUpload) } onclick = { runClick _}>Run Workflow</button>
      </section>
      <section class="item">
        <div class="ui labeled input">
          <div class="ui label">workflow wld</div>
          <input id ="wdl" onclick="this.value=null;" onchange = { uploadFileHandler(wdlFile) _ } accept=".wdl"  name="wdl" type="file" />
          </div>
      </section>
      <section class="item">
        <div class="ui labeled input">
          <div class="ui label">inputs json</div>
          <input id ="inputs" onclick="this.value=null;" onchange = { uploadFileHandler(inputs) _ } accept=".json" name="inputs" type="file" />
        </div>
      </section>
      <section class="item">
        <div class="ui labeled input">
          <div class="ui label">options (optional)</div>
          <input id ="options" onclick="this.value=null;"  onchange = { uploadFileHandler(options) _ } accept=".json"  name="options" type="file" />
        </div>
      </section>
    </div>

  val updater: Elem =
      <div class="ui menu">
      <section class="item">
        <div class="ui fluid action input">
          <div class={enabledIf("ui primary button", validUrl)} onclick={ updateClick _}>Update workflows</div>
          <input id="url" type="text" placeholder="Enter cromwell URL..."  oninput={ updateHandler _ } value={ url.dropRepeats } />
          <div class="ui small button" onclick={ localhostClick _ }>To default</div>
        </div>
      </section>
    </div>

}
