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
import diode.Dispatcher

import scala.xml.Elem


class RunnerView(dispatcher: Dispatcher) {

  val component: Elem =
    <table id="runs" class="ui blue table">
      <thead>
        <tr>
          <th>workflow WDL</th>
          <th>inputs</th>
          <th>options (optinal)</th>
          <th>run</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>
            <input id ="wdl" name="wdl" type="file" />
          </td>
          <td>
            <input id="inputs" name="inputs" type="file" />
          </td>
          <td>
            <input id="options" name="options" type="file" />
          </td>
          <td>
            <button class="ui primary button" onclick = { (()=> dispatcher.dispatch(Commands.Run("", "", "")))   }>Run</button>
          </td>
          </tr>
      </tbody>

    </table>
}
