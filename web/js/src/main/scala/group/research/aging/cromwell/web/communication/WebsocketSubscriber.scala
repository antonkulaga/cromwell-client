package group.research.aging.cromwell.web.communication

import group.research.aging.cromwell.web.utils.Events
import org.scalajs.dom
import org.scalajs.dom.Blob
import org.scalajs.dom.raw._

import scala.scalajs.js.typedarray.ArrayBuffer
import mhtml._

object WebsocketSubscriber {

  def fromRelativeURL(rel: String) = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    val url = s"$wsProtocol://${dom.document.location.host}/${rel}"
    new WebsocketSubscriber(url)
  }

}

class WebsocketSubscriber(url: String)
{

  lazy val w: WebSocket = new WebSocket(url)

  def send(message: String): Unit = w.send(message)
  def send(message: ArrayBuffer): Unit = w.send(message)
  def send(message: Blob): Unit = w.send(message)

  protected def subscribe(w: WebSocket) = {
    w.onopen = { event: Event ⇒ onOpen := event}
    w.onerror = { event: Event ⇒ onError := event}
    w.onmessage = { event: MessageEvent ⇒ onMessage := event}
    w.onclose = { event: Event ⇒  onClose := event}
  }

  private val emptyEvent = Events.createEvent()

  val onOpen: Var[dom.Event] = Var(emptyEvent)
  val onMessage: Var[dom.MessageEvent] = Var(Events.createMessageEvent())
  val onError: Var[dom.Event] = Var(emptyEvent)
  val onClose: Var[dom.Event] = Var(emptyEvent)

  subscribe(w)
}
