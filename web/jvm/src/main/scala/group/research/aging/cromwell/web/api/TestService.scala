package group.research.aging.cromwell.web.api

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._
import akka.pattern.ask
import akka.util.Timeout
import group.research.aging.cromwell.client.{CromwellClient, StatusInfo}
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.{ParameterIn, ParameterStyle}
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

@Path("/api")
class TestService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService with LocalWorkflows {


  @POST
  @Path("/test/{pipeline}")
  @Operation(summary = "tests the workflow", description = "test output of the workflow and returns its status and (with callback) the results",
    tags = Array("test"),
    requestBody = new RequestBody(
      content = Array(new Content(
        mediaType = "application/json",
        schema = new Schema(
          example = """{
"quantification.key": "0a1d74f32382b8a154acacc3a024bdce3709",
"quantification.samples_folder": "/data/samples",
"quantification.salmon_indexes": {
  "Bos taurus": "/data/indexes/salmon/Bos_taurus",
  "Heterocephalus glaber": "/data/indexes/salmon/Heterocephalus_glaber",
  "Rattus norvegicus": "/data/indexes/salmon/Rattus_norvegicus",
  "Caenorhabditis elegans": "/data/indexes/salmon/Caenorhabditis_elegans",
  "Homo sapiens": "/data/indexes/salmon/Homo_sapiens",
  "Drosophila melanogaster": "/data/indexes/salmon/Drosophila_melanogaster",
  "Mus musculus": "/data/indexes/salmon/Mus_musculus"
},
"quantification.samples": [
  "GSM1698568",
  "GSM1698570",
  "GSM2927683",
  "GSM2927750",
  "GSM2042593",
  "GSM2042596"
]
}"""))), description = "input JSON"),
    parameters = Array(
      new Parameter(name = "pipeline", in = ParameterIn.PATH, required = true,
        example = "quantification", style = ParameterStyle.DEFAULT, allowReserved = true,
        description = "path to the workflow inside pipelines folder (defined by PIPELINES enviroment variable, /data/pipelines by default)"),
      new Parameter(name = "server", in = ParameterIn.QUERY, required = false, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "URL of the cromwell server, enviroment variable CROMWELL by default"),
      new Parameter(name = "callback", style = ParameterStyle.SIMPLE,  allowReserved = true,
        in = ParameterIn.QUERY, required = false,
        description = "callback URL to report about the result of the query")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Workflow started, returns its ID",
        content = Array(new Content(schema = new Schema(example = """{
  "id": "e442e52a-9de1-47f0-8b4f-e6e565008cf1",
  "status": "Submitted"
}""")))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  def testRun: Route = pathPrefix("test" / Remaining) { pipeline =>
    debug(s"BEFORE PARAMETER EXTRACTION FOR ${pipeline}")
    extractPipeline(pipeline) match {
      case (None, _, _) =>
        error(s"CANNOT FIND ${pipeline}")
        reject(PipelinesRejections.PipelineNotFound(pipeline))

      case (Some(wdl), deps, _) =>
        parameters("server".?, "callback".?) { (serverOpt, callBackOpt) =>
          debug(s"FOUND PARAMETERS FOR RUNNING ${pipeline}")
          //val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
          val serverURL = serverOpt.getOrElse(CromwellClient.defaultURL)
          entity(as[String]) { json =>
            debug("Input JSON used:")
            debug(json)

            val toRun = Commands.TestRun(wdl, json, (getPipelineFile(pipeline) / "test.json").lines.mkString("\n"), deps) //TODO: fix problem

            val serverMessage = MessagesAPI.ServerCommand(toRun, serverURL, callBackOpt.map(Set(_)).getOrElse(Set.empty[String]))
            completeOrRecoverWith((runner ? serverMessage).mapTo[StatusInfo]) { extraction =>
              debug(s"running pipeline failed with ${extraction}")
              failWith(extraction) // not executed.
            }
          }
        } ~ {
          //debug("POST REQUEST!")
          //debug("WDL FOUND!")
          //debug(s"WDL IS: ${wdl}")
          if(deps.nonEmpty) debug(s"Found dependencies [${deps.map(_._1).mkString(", ")}]!")
          complete(HttpResponse(StatusCodes.OK, Nil, wdl))
        }
    }
  }


  def routes: Route  = testRun
}