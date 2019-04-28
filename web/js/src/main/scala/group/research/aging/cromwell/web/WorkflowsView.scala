package group.research.aging.cromwell.web
//import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{LogCall, Metadata, WorkflowFailure}
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.xml.Elem

import java.time.format.DateTimeFormatter

class WorkflowsView(allMetadata: Rx[List[Metadata]], baseHost: Rx[String], commands: Var[Commands.Command])
{

  def clientPort = dom.window.location.port match {
    case "" => ""
    case v => ":" + v
  }

  val host: Rx[String] = baseHost.map(h => dom.window.location.protocol +"//"+ h + clientPort)
  //val allMetadata: Var[List[Metadata]]  = Var(initialMetadata)

  var desc = true

  def timingURL(base: String, id: String): String = base + s"/api/workflows/v1/${id}/timing"

  /*
  def onMetadataUpdate(reader: ModelRO[List[Metadata]]): Unit = {
    allMetadata := (if(desc) reader.value.reverse else reader.value)
  }
 colspan="3"
  def onHostUpdate(reader: ModelRO[String]): Unit = {
    host := reader.value
  }
  */


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
        <th  colspan="2">workflow</th>
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
      {allMetadata.map(meta=> meta.sortWith{
      case (a, b) =>
        val isParent = b.parentWorkflowId.isDefined && b.parentWorkflowId.get == a.id || b.rootWorkflowId.isDefined && b.rootWorkflowId.get == a.id
        val notChild = a.parentWorkflowId.isEmpty || (a.parentWorkflowId.get != b.id && a.rootWorkflowId.isDefined && a.rootWorkflowId.get != b.id)
          isParent ||
            (notChild &&
          (
            for{
            sa <- a.start
            sb <- b.start
            } yield sa.isAfter(sb)).getOrElse(false)
          )
    }.map(r=>metadataRow(r)))}
    </tbody>
  </table>

  def statusClass(str: String): String = str.toLowerCase match {
    case "succeeded" | "done" => "positive"
    case "failed" | "aborted" => "negative"
    case _ => "warning"
  }


  def metadataRow(r: Metadata): Elem = {
    <tr class={statusClass(r.status)}>
      {if(r.parentWorkflowId.isDefined) <td style="border: 0px !important;"></td> else <!--no cell-->}
      <td colspan={if(r.parentWorkflowId.isDefined) "1" else "2"} style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>{generalInfo(r)}{rowInputs(r)}{rowOutputs(r)}</td>
      <td style={if(r.parentWorkflowId.isDefined) "border: 0px !important;" else ""}>
        {rowFailures(r)}{rowCallsTable(r)}
      </td>
    </tr>
  }
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
          <th>starts</th><td><h3><a href={host.map(h=> timingURL(h, r.id))} target ="_blank">{r.start.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}</a></h3></td>
          <th>ends</th><td><h3><a href={host.map(h=> timingURL(h, r.id))} target ="_blank">{r.end.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}</a></h3></td>
        </tr>

        <tr>
          <th>id</th><td >{r.id}</td>
          <th>date</th><td>{r.dates}</td>
        </tr>
        <tr>
          <th >root</th><td colspan="3"><a href={host.map(h=> h + r.workflowRoot)}  target ="_blank">{r.workflowRoot}</a></td>
        </tr>
        {(if(r.parentWorkflowId.isDefined)
        <tr>
        <th>parents</th> <td colspan="3"> {r.rootWorkflowId.map(v=>if(v == r.parentWorkflowId.get) "" else v + "/").getOrElse("") + r.parentWorkflowId.get}
        </td>
      </tr> else <br></br>)
        }
      </tbody>
    </table>

  def un(str: String): String = str.replace("\\\"","")
  private def messageClass(r: Metadata, tp: String = "positive") = if(r.parentWorkflowId.isDefined) s"ui ${tp} tiny message" else s"ui ${tp} small message"

  def rowInputs(r: Metadata): Elem =
    <div class={messageClass(r, "info")}>
      <div class="header">Inputs:</div>
      <div class="ui list">
        {
          <code class="item">
            { r.inputs.spaces4 }
          </code>
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
        .flatMap(kv=> callRow(kv._1, kv._2, host))}
      </tbody>
    </table>
  else <br/>

  def callRow(name: String, calls: List[LogCall], fileHost: Rx[String]): List[Elem] =
      calls.map(c=>
        <tr>
          <td><a href={fileHost.map(h=> h + c.callRoot)} target="_blank">{name}</a></td>
          <td class={statusClass(c.executionStatus)}>{c.executionStatus}</td>
          <td><a href={fileHost.map(h=> h + c.stdout)}  target ="_blank">{c.stdout}</a></td>
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


}

