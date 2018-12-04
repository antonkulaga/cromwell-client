package group.research.aging.cromwell.web
//import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{LogCall, Metadata, WorkflowFailure}
import mhtml._
import org.scalajs.dom.Event
import scala.xml.Elem


class WorkflowsView(allMetadata: Rx[List[Metadata]], host: Rx[String], commands: Var[Commands.Command])
{
  //val allMetadata: Var[List[Metadata]]  = Var(initialMetadata)

  var desc = true

  def timingURL(base: String, id: String): String = base + s"/api/workflows/v1/${id}/timing"



  /*
  def onMetadataUpdate(reader: ModelRO[List[Metadata]]): Unit = {
    allMetadata := (if(desc) reader.value.reverse else reader.value)
  }

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
        <th>workflow</th>
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
      {allMetadata.map(meta=> meta.map(r=>metadataRow(r)))}
    </tbody>
  </table>

  def statusClass(str: String): String = str.toLowerCase match {
    case "succeeded" | "done" => "positive"
    case "failed" | "aborted" => "negative"
    case _ => "warning"
  }


  def metadataRow(r: Metadata): Elem =
        <tr class={statusClass(r.status)}>
          <td>
            {generalInfo(r)}
            {rowInputs(r)}
            {rowOutputs(r)}
          </td>
          <td>
            {rowFailures(r)}
            {rowCallsTable(r)}
          </td>
        </tr>

  protected def abort(id: String)(event: Event): Unit = {

    commands := Commands.Abort(id)
  }

  def generalInfo(r: Metadata): Elem =
      <table id="workflows" class="ui small padded striped celled table">
      <tbody>
        <tr>
          <th>name/id</th><td class={statusClass(r.status)}><h3>{r.workflowName.getOrElse("NO NAME")}</h3>{r.id}</td>
          <th>status</th> <td class={statusClass(r.status)}><h3>{r.status}</h3>{
            {if(r.status == "Running") <button class="ui button"  onclick={ abort(r.id) _}><i class="stop icon"></i></button> else <span/>}
          }</td>
        </tr>
        <tr>
          <th>starts</th><td><h3><a href={host.map(h=> timingURL(h, r.id))} target ="_blank">{r.startTime}</a></h3></td>
          <th>ends</th><td><h3><a href={host.map(h=> timingURL(h, r.id))} target ="_blank">{r.endTime}</a></h3></td>
        </tr>

        <tr>
          <th>id</th><td >{r.id}</td>
          <th>date</th><td>{r.dates}</td>
        </tr>
        <tr>
          <th >root</th><td colspan="3"><a href={host.map(h=> h + r.workflowRoot.getOrElse(""))}  target ="_blank">{r.workflowRoot.getOrElse("")}</a></td>
        </tr>
      </tbody>
    </table>

  def un(str: String): String = str.replace("\\\"","")

  def rowInputs(r: Metadata): Elem =
    <div class="ui info message">
      <div class="header">Inputs:</div>
      <div class="ui list">
        {
        r.inputs.values.toList.map(kv=>
          <div class="item">
            { un(kv._1) + " = " + un(kv._2) }
          </div>
        )
        }
      </div>
    </div>

  def rowOutputs(r: Metadata): Elem = if(r.outputs.values.isEmpty) <br/> else
    <div class="ui positive message">
      <div class="header">Outputs:</div>
      <div class="ui list">
        {
        r.outputs.values.toList.map(kv=>
          <div class="item">
            { un(kv._1) + " = " + un(kv._2) }
          </div>
        )
        }
      </div>
    </div>

  def rowCallsTable(r: Metadata): Elem = if(r.calls.isDefined && r.calls.get.nonEmpty)
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
        {r.calls.map(_.toList).getOrElse(Nil)
        .flatMap(kv=> callRow(kv._1, kv._2, host))}
      </tbody>
    </table>
  else <br/>

  def callRow(name: String, calls: List[LogCall], fileHost: Rx[String]): List[Elem] =
      calls.map(c=>
        <tr>
          <td><a href={fileHost.map(h=> h + c.callRoot.getOrElse(""))} target="_blank">{name}</a></td>
          <td class={statusClass(c.executionStatus.getOrElse(""))}>{c.executionStatus}</td>
          <td><a href={fileHost.map(h=> h + c.stdout.getOrElse(""))}  target ="_blank">{c.stdout}</a></td>
          <td><a href={fileHost.map(h=> h + c.stderr.getOrElse(""))} target ="_blank">{c.stderr}</a></td>
          <td>{c.callCaching.fold("")(r=>r.result.getOrElse(""))}</td>
          <td>{c.shardIndex}</td>
        </tr>
      )

  def rowFailures(r: Metadata): List[Elem] = r.failures.getOrElse(List.empty[WorkflowFailure]).map(f=>
    <div class="ui negative message">
      {f.message}
      <p> {f.causedBy.mkString}</p>
    </div>)


}

