package group.research.aging.cromwell.web.api

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._
import akka.pattern.ask
import akka.util.Timeout
import better.files.File
import group.research.aging.cromwell.client.{CromwellClient, StatusInfo}
import group.research.aging.cromwell.web.Commands
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Service to run cromwell pipelines
  * @param materializer
  */
@Path(value = "/api")
class RunService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService {


  @POST
  @Path("/run")
  @Operation(summary = "runs the workflow", description = "runs the workflow and returns its status",
    requestBody = new RequestBody(
      content = Array(new Content(schema = new Schema(
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
      new Parameter(name = "wdl", in = ParameterIn.PATH, required = true,
        example = "quantification",
        description = "path to the workflow inside pipelines folder (defined by PIPELINES enviroment variable, /data/pipelines by default)"),
      new Parameter(name = "server", in = ParameterIn.QUERY, required = false,
        example = "http://pic:8000",
        description = "URL of the cromwell server, enviroment variable CROMWELL by default"),
      new Parameter(name = "callback", in = ParameterIn.QUERY, required = false,
        description = "callback URL to report about the result of the query")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Workflow started, returns its ID",
        content = Array(new Content(schema = new Schema(example = """{
  "id": "e442e52a-9de1-47f0-8b4f-e6e565008cf1",
  "status": "Submitted"
}""")))),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  def runAPI: Route = pathPrefix("run" / Remaining) { wdl =>
    val root = File(scala.util.Properties.envOrElse("PIPELINES", "/data/pipelines" ))
    val fl = if(root.exists) {
      if( (root / (wdl + ".wdl")).exists) root / (wdl + ".wdl") else root / wdl
    } else File(wdl)
    if (fl.notExists) {
      if(root.exists) getFromBrowseableDirectories(root.pathAsString) else {
        error(s"CANNOT FIND ${fl.pathAsString}")
        reject(PipelinesRejections.PipelineNotFound(fl.pathAsString))
      }
    }
    else {
      parameters("server".?, "callback".?) { (serverOpt, callBackOpt) =>
        //val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
        val serverURL = serverOpt.getOrElse(CromwellClient.defaultURL)
        entity(as[String]) { json =>
          //c.postWorkflowStrings(wdl, json.spaces4)
          //debug("JSON RECEIVED!!!")
          //debug("running the following WDL:")
          //debug(fl.contentAsString)
          //debug("---------------------------")
          debug("Input JSON used:")
          debug(json)
          val wdl = fl.contentAsString
          val toRun = Commands.Run(wdl, json, "")
          val serverMessage = MessagesAPI.ServerCommand(toRun, serverURL, callBackOpt.map(Set(_)).getOrElse(Set.empty[String]))
          completeOrRecoverWith((runner ? serverMessage).mapTo[StatusInfo]) { extraction =>
            debug(s"running pipeline failed with ${extraction}")
            failWith(extraction) // not executed.
          }
        }
      } ~ {
        debug("POST REQUEST!")
        debug("WDL FOUND!")
        debug(s"WDL IS: ${fl.contentAsString}")
        complete(HttpResponse(StatusCodes.OK, Nil, fl.contentAsString))
      }
    }
  }

  def routes: Route = runAPI

}
