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

class UploaderView {

  val component =
    <table id="workflows" class="ui blue sortable table">
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
          <input id ="wdl" name="wdl" type="file" />
        </tr>
        <tr>
          <input id="inputs" name="inputs" type="file" />
        </tr>
        <tr>
          <input id="options" name="options" type="file" />
        </tr>
        <tr>
          <button class="ui primary button">Run</button>
        </tr>

      </tbody>

    </table>

    <div class="ui segment">

    </div>
}
