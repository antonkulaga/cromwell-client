package group.research.aging.cromwell.web

/*
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.BindingInstances.applicativeSyntax._

import com.thoughtworks.binding.{Binding, dom}

import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.Metadata
import org.scalajs.dom.raw.Node
import scalaz.std.list._
import scalaz.std._
import scalaz._
import scalaz.std.scalaFuture._


class WorkflowBindingView(allMetadata: Binding[Seq[Metadata]], baseHost: Binding[String]) extends WorkflowViewBase {


  //val host: Binding[String] = baseHost.map(h => org.scalajs.dom.window.location.protocol +"//"+ h + clientPort)
  @dom val metas= allMetadata.map(ms => ms.map(m=>metadataRow(m))).bind

  @dom
  def metadataRow(r: Metadata): Binding[Node] = {
    <tr class={statusClass(r.status)}>
      {if(r.parentWorkflowId.isDefined) <td style="border: 0px !important;">
      <i class={if(r.rootWorkflowId.isDefined && r.rootWorkflowId.contains(r.parentWorkflowId.get)) "angle right icon" else "small angle double right icon"}></i>
    </td> else <!--no cell-->}
      <td colspan={if(r.parentWorkflowId.isDefined) "1" else "2"} style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>
      <h1>SOME INFO</h1>
      </td>
      <td style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>
      <h1>TEST</h1>
      </td>
    </tr>
  }


  @dom
  def rowCallsTable(r: Metadata): Binding[Node] = if(r.calls.nonEmpty)
    <table class="ui small collapsing table">
      <thead>
        <tr>
          <th>name</th>
          <th>status</th>
          <th>stdout</th>
          <th>stderr</th>
          <th>cache</th>
          <th>shard</th>
        </tr>
      </thead>
      <tbody>
        <h1>Call row!</h1>
      </tbody>
    </table> else
    <span></span>

  @dom
  def table: Binding[Node] =   <table id="workflows2" class="ui small blue striped celled table">
    <thead>
      <tr>
        <th>workflow</th>
        <th>calls and failures</th>
      </tr>
    </thead>
    <tbody>
      {
      metas.bind
      }
    </tbody>
  </table>

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


  def init(node: Node) = {
    val div = org.scalajs.dom.document.getElementById("test")
    println("INIT WORKS!!!")
    dom.render(node, table)
  }

}
*/