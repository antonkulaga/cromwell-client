package group.research.aging.cromwell.web
//import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{LogCall, Metadata, WorkflowFailure}
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.{Event, console, window}

import java.net.URI
import scalajs.js
import scala.xml.Elem
import java.time.format.DateTimeFormatter
//import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._


class WorkflowsView(allMetadata: Rx[List[Metadata]], url: Rx[URI],
                    commands: Var[Commands.Command], filePrefixUrl: Rx[Option[String]]) extends WorkflowViewBase
{

  //val host: Rx[String] =url.map(h => dom.window.location.protocol +"//"+ window.location.host + clientPort)
  val fileBase = for{
    u <- url
    f <- filePrefixUrl
  } yield {
    //dom.console.log("CLIENT PORT IS ",clientPort)

    f.getOrElse{
      dom.window.location.protocol +"//"+ u.getHost + clientPort
    }
  }


  def metadataRow(r: Metadata): Elem = {
    <tr class={statusClass(r.status)}>
      {if(r.parentWorkflowId.isDefined) <td style="border: 0px !important;">
      <i class={if(r.rootWorkflowId.isDefined && r.rootWorkflowId.contains(r.parentWorkflowId.get)) "angle right icon" else "small angle double right icon"}></i>
    </td> else <!--no cell-->}
      <td class="workflow_cell" colspan={if(r.parentWorkflowId.isDefined) "1" else "2"} style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>{generalInfo(r)}{rowInputs(r)}{rowOutputs(r)}</td>
      <td style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>
        {rowFailures(r)}{rowCallsTable(r)}
      </td>
    </tr>
  }


  def rowInputs(r: Metadata): Elem =
    <div class={messageClass(r, "info")}>
      <div class="header">Inputs:</div>
      <div class="ui list">
        {

        <pre class="item" style="max-height: 60vh; overflow-y:scroll;">
          {
          r.inputs.spaces4
          }
        </pre>
        }
      </div>
    </div>


  def rowOutputs(r: Metadata): Elem = if(r.outputs.isNull) <br/> else
    <div class={messageClass(r)}>
      <div class="header">Outputs:</div>
      <code class="ui list">
        {  r.outputs.spaces4 }
      </code>
    </div>


  def callRow(name: String, calls: List[LogCall], fileHost: Rx[String]): List[Elem] =
    calls.map(c=>
      <tr>
        <td><a href={fileHost.map(h=> h + c.callRoot)} target="_blank">{name}</a></td>
        <td class={statusClass(c.executionStatus)}>{c.executionStatus}</td>
        <td><a href={fileHost.map(h=>
          {h + c.stdout})}  target ="_blank">{c.stdout}</a></td>
        <td><a href={fileHost.map(h=> h + c.stderr)} target ="_blank">{c.stderr}</a></td>
        <td>{c.callCaching.fold("")(r=>r.result)}</td>
        <td>{c.shardIndex}</td>
      </tr>
    )

  def rowFailures(r: Metadata): List[Elem] = r.failures.map(f=>
    <div class="ui negative message">
      {f.message}
      <p> {f.causedBy.mkString}</p>
    </div>)



  var desc = true

  def timingURL(base: String, id: String): String = base + s"/api/workflows/v1/${id}/timing"


  """
    |  workflowName: String,
    |                                workflowRoot: String,
    |                                id: String,
    |                                submission: String,
    |                                status: String,
    |                                start: String,
    |                                end: String, //ISO_INSTANT
    |                                inputs: Inputs,
    |                                failures: List[WorkflowFailure],
    |                                submittedFiles: SubmittedFiles,
    |
  """.stripMargin

  val component: Elem =
    <table id="workflows" class="ui small blue striped celled table">
    <thead>
      <tr>
        <th class="workflow_cell" colspan="2">workflow</th>
        <th>calls and failures</th>
      </tr>
    </thead>
    {
      allMetadata.map{meta =>
        if(meta.isEmpty)
          <div class="ui info message">
            <div class="header">No workflows in the history</div>
          </div>
        else <!-- -->
      }
    }
    <tbody>
      {allMetadata.dropRepeats.map(meta=> meta.sortWith{
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
            } yield sa.isAfter(sb))
            .getOrElse({
              a.status != "Aborted"
            })
            //.getOrElse(false)
          )
    }.map(r=>metadataRow(r)))}
    </tbody>
  </table>


  protected def abort(id: String)(event: Event): Unit = {

    commands := Commands.Abort(id)
  }

  private def workflowHeader(r: Metadata)(fun: Metadata=>String): Elem = if(r.parentWorkflowId.isDefined) <b>{fun(r)}</b> else <h3>{fun(r)}</h3>

  def generalInfo(r: Metadata): Elem =
      <table id="workflows" class="ui tiny padded striped celled table">
      <tbody>
        <tr>
          <th>name/id</th><td class={statusClass(r.status)}>{workflowHeader(r)(m=>m.workflowName)}</td>
          <th>status</th> <td class={statusClass(r.status)}>{workflowHeader(r)(m=>m.status)}{
            {if(r.status == "Running") <button class="ui button"  onclick={ abort(r.id) _}><i class="stop icon"></i></button> else <span/>}
          }</td>
        </tr>
        <tr>
          <th>starts</th><td><h3><a href={url.map(u=> timingURL(u.toASCIIString, r.id))} target ="_blank">{r.start.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}</a></h3></td>
          <th>ends</th><td><h3><a href={url.map(u=> timingURL(u.toASCIIString, r.id))} target ="_blank">{r.end.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}</a></h3></td>
        </tr>

        <tr>
          <th>id</th><td >{r.id}</td>
          <th>date</th><td>{r.dates}</td>
        </tr>
        <tr>
          <th >root</th><td colspan="3"><a href={fileBase.map(f=> f + r.workflowRoot)}  target ="_blank">{r.workflowRoot}</a></td>
        </tr>
        {(if(r.parentWorkflowId.isDefined)
        <tr>
        <th>parents</th> <td colspan="3"> {r.rootWorkflowId.map(v=>if(v == r.parentWorkflowId.get) "" else v + " / ").getOrElse("") + r.parentWorkflowId.get}
        </td>
      </tr> else <br></br>)
        }
      </tbody>
    </table>

  def rowCallsTable(r: Metadata): Elem = if(r.calls.nonEmpty)
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
        {r.calls.toList
        .flatMap{ case (key, value)=> callRow(key, value, fileBase)}}
      </tbody>
    </table>
  else <br/>

}

