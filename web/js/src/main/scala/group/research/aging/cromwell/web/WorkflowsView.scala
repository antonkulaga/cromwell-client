package group.research.aging.cromwell.web
import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{LogCall, Metadata, WorkflowFailure}
import mhtml.{Rx, Var}

import scala.scalajs.js
import scala.xml.Elem

class WorkflowsView(initialMetadata: List[Metadata], host: Var[String])
{
  val allMetadata: Var[List[Metadata]]  = Var(initialMetadata)

  var desc = true

  def onMetadataUpdate(reader: ModelRO[List[Metadata]]): Unit = {
    allMetadata := (if(desc) reader.value.reverse else reader.value)
  }

  def onHostUpdate(reader: ModelRO[String]): Unit = {
    host := reader.value
  }

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

  val component: Elem = <table id="workflows" class="ui small blue striped celled table">
    <thead>
      <tr>
        <th>workflow</th>
        <th>status</th>
        <th>dates</th>
        <th>start</th>
        <th>end</th>
        <th>inputs</th>
        <th>failures and calls</th>
      </tr>
    </thead>
    <tbody>
      {allMetadata.map(meta=> meta.map(r=>metadataRow(r)))}
    </tbody>
  </table>

  def statusClass(str: String) = str.toLowerCase match {
    case "succeeded" | "done" => "positive"
    case "failed" => "negative"
    case _ => "warning"
  }

  def metadataRow(r: Metadata) =
        <tr class={statusClass(r.status)}>
          <td class={statusClass(r.status)}>
            <strong>{r.workflowName.getOrElse("NO NAME")}</strong> <br></br>{r.id}
          </td>
          <td class={statusClass(r.status)}>{r.status}</td>
          <td>{r.dates}</td>
          <td>{r.startTime}</td>
          <td>{r.endTime}</td>
          <td>{rowInputs(r)}</td>
          <td>
            {rowFailures(r)}
            {rowCallsTable(r)}
          </td>
        </tr>

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
          <td>{name}</td>
          <td class={statusClass(c.executionStatus)}>{c.executionStatus}</td>
          <td><a href={fileHost.map(h=> h + c.stdout)}>{c.stdout}</a></td>
          <td><a href={fileHost.map(h=> h + c.stderr)}>{c.stderr}</a></td>
          <td>{c.callCaching.result}</td>
          <td>{c.shardIndex}</td>
        </tr>
      )

  def rowFailures(r: Metadata): List[Elem] = r.failures.getOrElse(List.empty[WorkflowFailure]).map(f=>
    <div class="ui negative message">
      {f.message}
      <p> {f.causedBy.mkString}</p>
    </div>)

  def rowInputs(r: Metadata): Elem =
      <div class="ui info message">
        <div class="ui list">
          {
          r.inputs.values.toList.map(kv=>
            <div class="item">
              { kv._1 + " = " + kv._2 }
            </div>
          )
          }
        </div>
      </div>


}

