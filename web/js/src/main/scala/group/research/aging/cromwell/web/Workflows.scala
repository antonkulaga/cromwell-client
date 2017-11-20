package group.research.aging.cromwell.web
import diode.{Dispatcher, ModelRO}
import group.research.aging.cromwell.client.{Metadata, WorkflowFailure}
import mhtml.Var

import scala.scalajs.js
import scala.xml.Elem

class Workflows( initialMetadata: List[Metadata])
{
  val allMetadata: Var[List[Metadata]]  = Var(initialMetadata)

  var desc = true

  def onUpdate( reader: ModelRO[List[Metadata]]): Unit = {
    allMetadata := (if(desc) reader.value.reverse else reader.value)
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

  val component: Elem = <table id="workflows" class="ui small blue table">
    <thead>
      <tr>
        <th>workflow</th>
        <th>status</th>
        <th>dates</th>
        <th>start</th>
        <th>end</th>
        <th>failures</th>
        <th>inputs</th>
      </tr>
    </thead>
    <tbody>
      {
      allMetadata.map(meta=>meta.map(r=>
        <tr>
          <td>
            {r.workflowName.getOrElse("NO NAME")} <br></br>{r.id}
          </td>
          <td>{r.status}</td>
          <td>{r.dates}</td>
          <td>{r.startTime}</td>
          <td>{r.endTime}</td>
          <td>{
            r.failures.getOrElse(List.empty[WorkflowFailure]).map(f=>
              <div class="ui negative message">
                {f.message}
                <p> {f.causedBy.mkString}</p>
              </div>
            )
            }</td>
          <td>
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
          </td>
        </tr>

      )
      )
      }
    </tbody>
  </table>
}
