package group.research.aging.cromwell.web

import diode._
import org.scalajs.dom

class LoggingProcessor[M <: AnyRef] extends ActionProcessor[M] {
  var log = Vector.empty[(Long, String)]
  def process(dispatch: Dispatcher, action: Any, next: Any => ActionResult[M], currentModel: M): ActionResult[M] = {
    // log the action
    val actionLog = (System.currentTimeMillis(), action.toString)
    log = log :+ actionLog
    println(actionLog)
    next(action)
  }
}