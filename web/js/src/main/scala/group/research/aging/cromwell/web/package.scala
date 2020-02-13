package group.research.aging.cromwell

import org.querki.jquery.JQuery

import scala.scalajs.js

package object web {

  @js.native
  trait JQuerySemanticUI extends JQuery {
    def tablesort()(): this.type = js.native
  }

  implicit def jq2semanticUI(jq: JQuery): JQuerySemanticUI =
    jq.asInstanceOf[JQuerySemanticUI]

}
