package group.research.aging.cromwell.web

import group.research.aging.cromwel.client.CromwellClient
import group.research.aging.cromwell.client.{Metadata, QueryResults}
import org.querki.jquery._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

object CromwellWeb extends scala.App {

    import mhtml._
    import org.scalajs.dom

    lazy val table: JQuery = $("workflows")

    val updateEach = Var(5) //seconds

    //val url = Var("http://agingkills.westeurope.cloudapp.azure.com")

    var client = new CromwellClient("http://agingkills.westeurope.cloudapp.azure.com", "v1")

    val queryResults = Var(QueryResults.empty)

    def query(): Unit = {
      client.getQuery().onComplete{
        case Success(results) => queryResults := results
        case Failure(th) => dom.console.error(th.getMessage)
      }
    }

    val allMetadata = Var(List.empty[Metadata])

    def updateClick(): Unit = {
      //if(client.base != url.now) client = new CromwellClient("http://agingkills.westeurope.cloudapp.azure.com", "v1")
      metadataUpdate()
    }

    def metadataUpdate(): Unit = {

      client.getAllMetadata().onComplete{
        case Success(results) =>
          allMetadata := results
          //table.tablesort()
        case Failure(th) => dom.console.error(th.getMessage)
      }
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

    val component =
      <div class="ui teal segment">
        <div class="ui equal width grid">
          <div class="column"><div class="ui input"></div></div>
          <div class="column"><button onclick={ () => this.updateClick()}>Update</button></div>
        </div>
              <table id="workflows" class="ui blue sortable table">
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
      </div>

    val div = dom.document.getElementById("cromwell")
    mount(div, component)

}
