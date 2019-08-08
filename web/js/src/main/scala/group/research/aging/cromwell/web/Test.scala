package group.research.aging.cromwell.web

import com.thoughtworks.binding
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding._
import group.research.aging.cromwell.client
import group.research.aging.cromwell.web.Results.QueryWorkflowResults
import org.scalajs.dom.html.{Table, TableRow}
import org.scalajs.dom.raw.{Event, Node}
import scalaz.std.list._
import scalaz.std._

object Test {

  val state = Var(State.empty)

  @dom
  def metaRow(m: client.Metadata): Binding[Node]=
    <tr>
      <td>
        {m.workflowName}
      </td>
      <td>
        <td>
          {m.workflowName}
        </td>
      </td>
    </tr>


  def sortMeta(meta: List[client.Metadata]): List[client.Metadata] =
    meta.sortWith{
      case (a, b) =>
        val isParent = b.parentWorkflowId.isDefined &&
          (b.parentWorkflowId.get == a.id || b.rootWorkflowId.get == a.id)
        val notChild = a.parentWorkflowId.isEmpty || (a.parentWorkflowId.get != b.id && a.rootWorkflowId.isDefined && a.rootWorkflowId.get != b.id)
        isParent ||
          (notChild &&
            (
              for{
                sa <- a.start
                sb <- b.start
              } yield sa.isAfter(sb)).getOrElse(false)
            )
  }


  @dom
  def table =   <table id="workflows2" class="ui small blue striped celled table">
    <thead>
      <tr>
        <th>workflow</th>
        <th>calls and failures</th>
      </tr>
    </thead>
    <tbody>
      {for (meta <- Constants(sortMeta(state.bind.results.metadata.values.toList): _*)) yield {metaRow(meta).bind}}
    </tbody>
    </table>
  /*
      <table border="1" cellPadding="5">
        <thead>
          <tr>
            <th>Name</th>
            <th>E-mail</th>
            <th>Operation</th>
          </tr>
        </thead>
        <tbody>
          <tr><td><h1>?????????????????????????????????????????</h1></td></tr>
          { for ((key, mp) <- Constants(state.bind.results.metadata.toSeq:_*))  yield <td>{key}</td>
          }

        </tbody>
      </table>
*/

  def init(node: Node, stateExt: mhtml.Var[State]) = {
    val div = org.scalajs.dom.document.getElementById("test")
    stateExt.impure.run{ s=> state.value = s}
    println("INIT WORKS!!!")
    dom.render(node, table)
  }
}
