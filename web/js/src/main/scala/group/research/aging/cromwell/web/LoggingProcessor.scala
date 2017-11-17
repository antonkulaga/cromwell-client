package group.research.aging.cromwell.web

import diode._
import org.scalajs.dom

class LoggingProcessor[M <: AnyRef] extends ActionProcessor[M] {
  var log = Vector.empty[(Long, String)]
  def process(dispatch: Dispatcher, action: Any, next: Any => ActionResult[M], currentModel: M): ActionResult[M] = {
    // log the action
    log = log :+ (System.currentTimeMillis(), action.toString)
    pprint.pprintln((System.currentTimeMillis(), action.toString))
    // call the next processor
    next(action)
  }
}