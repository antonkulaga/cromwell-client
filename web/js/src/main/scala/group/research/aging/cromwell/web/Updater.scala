package group.research.aging.cromwell.web

import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{Metadata, QueryResults}
import mhtml.Var
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Failure, Success}
import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{Metadata, QueryResults}
import org.querki.jquery._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}
import mhtml._
import cats._
import cats.implicits._

class Updater(val workflowsInfo: Var[List[Metadata]]) {

  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  val counter = Var(0)

  lazy val defURL = "http://agingkills.westeurope.cloudapp.azure.com"

  var url = Var(defURL)
  val autoUpdate = Var(0)

  def handler(event: js.Dynamic): Unit = {
    val str = event.target.value.asInstanceOf[String]
    url := (if(str=="") defURL else str)
  }

  def getClient() = new CromwellClient(url.now, "v1")

  val queryResults = Var(QueryResults.empty)

  def query(): Unit = {
    val client = getClient()
    client.getQuery().onComplete{
      case Success(results) => queryResults := results
      case Failure(th) => dom.console.error(th.getMessage)
    }
  }

  def updateClick(): Unit = {
    //if(client.base != url.now) client = new CromwellClient("http://agingkills.westeurope.cloudapp.azure.com", "v1")
    metadataUpdate()
  }

  def metadataUpdate(): Unit = {
    val client = getClient()
    client.getAllMetadata().onComplete{
      case Success(results) =>
        workflowsInfo := results
      //table.tablesort()
      case Failure(th) => dom.console.error(th.getMessage)
    }
  }

  val component =

    <div class="ui menu">
      <div class="item">
        <input class="ui input" type="text"
               placeholder="Enter cromwell URL..."
               oninput={ handler _ }/>
      </div>
      <div class="item">
        <div class="ui primary button" onclick={ () => this.updateClick()}>Update</div>
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
    </div>
}
