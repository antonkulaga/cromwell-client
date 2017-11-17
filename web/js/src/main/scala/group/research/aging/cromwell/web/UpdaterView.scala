package group.research.aging.cromwell.web

import cats.implicits._
import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.QueryResults
import mhtml._

import scala.scalajs.js

class UpdaterView(dispatch: Dispatcher) {

  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  val counter = Var(0)

  lazy val defURL = "http://agingkills.westeurope.cloudapp.azure.com"

  protected var url = Var(defURL)


  val autoUpdate = Var(0)

  def onUrlUpdate(reader: ModelRO[String]): Unit = {
    url := reader.value
  }


  protected def handler(event: js.Dynamic): Unit = {
    val str = event.target.value.asInstanceOf[String]
    url := str
  }

  val queryResults = Var(QueryResults.empty)

  def updateClick(): Unit = {
    //if(client.base != url.now) client = new CromwellClient("http://agingkills.westeurope.cloudapp.azure.com", "v1")
    dispatch(Commands.ChangeClient(url.now))
    dispatch.dispatch(Commands.GetMetadata())
  }


  val component =
    <div class="ui menu">
      <div class="item">
        <div class="ui fluid action input">
          <input id="url" type="text" placeholder="Enter cromwell URL..."  oninput={ handler _ } value={ url.dropRepeats } />
            <div class="ui primary button" onclick={ () => this.updateClick()}>Update</div>
        </div>
      </div>
      <!--
      <div class="item">
      </div>
      <div class="item">
        <div class="ui right labeled input">
          <div>
            <button onclick={() => counter.update(_ - 1)}>-</button>
            {counter}
            <button onclick={() => counter.update(_ + 1)}>+</button>
          </div>
          <div class="ui basic label">
            update interval
          </div>
        </div>
      </div>
      -->
    </div>
}
