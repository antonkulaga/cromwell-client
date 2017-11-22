package group.research.aging.cromwell.web

import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{Metadata, QueryResults}
import org.querki.jquery._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}
import mhtml._
import cats._
import cats.implicits._


object CromwellWeb extends scala.App {

  import mhtml._
  import org.scalajs.dom

  lazy val table: JQuery = $("workflows")

  AppCircuit.addProcessor(new LoggingProcessor[AppModel]())
  val updater = new RunnerView(AppCircuit)
  val workflows = new Workflows(AppCircuit.zoom(_.metadata).value, Var(AppCircuit.zoom(_.client.base).value))
  val errors = new ErrorsView(AppCircuit)

  AppCircuit.subscribe(AppCircuit.zoom(_.metadata))(workflows.onMetadataUpdate)
  AppCircuit.subscribe(AppCircuit.zoom(_.client.base))(workflows.onHostUpdate)
  AppCircuit.subscribe(AppCircuit.zoom(_.errors))(errors.onUpdate)


  val component =
    <div id="cromwell">
      {  updater.runner  }
      {  updater.updater  }
      {  errors.component }
      {  workflows.component }
    </div>

  val div = dom.document.getElementById("main")
  mount(div, component)

}
