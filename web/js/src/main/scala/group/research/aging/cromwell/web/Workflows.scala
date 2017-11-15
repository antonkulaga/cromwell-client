package group.research.aging.cromwell.web
import group.research.aging.cromwell.client.Metadata
import mhtml.Var

class Workflows(allMetadata: Var[List[Metadata]]) {

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

  val component = <table id="workflows" class="ui blue sortable table">
    <thead>
      <tr>
        <th>workflow</th>
        <th>status</th>
        <th>duration</th>
        <th>failures</th>
        <th>inputs</th>
      </tr>
    </thead>
    <tbody>
      {
      allMetadata.map(meta=>meta.map(r=>
        <tr>
          <td>
            {r.workflowName} <br></br>{r.id}
          </td>
          <td>{r.status}</td>
          <td>{r.start} - {r.end}</td>
          <td>{
            r.failures.map( f=>
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
