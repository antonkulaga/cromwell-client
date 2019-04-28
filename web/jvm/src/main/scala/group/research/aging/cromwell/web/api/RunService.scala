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
import io.swagger.v3.oas.annotations.enums.{ParameterIn, ParameterStyle}
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._
import wvlet.log.LogSupport

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


/**
  * Service to run cromwell pipelines
  * @param materializer
  */
@Path("/api")
class RunService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService with LocalWorkflows {

  private def concatJson(defaults: String, json: String) = {
    val d = defaults.trim
    if(d.endsWith("}"))
      d.replaceFirst("}$", ",") + json.trim.replaceAll("^\\{", "\n")
      else
    {
      d + json
    }

  }
  
  @POST
  @Path("/run/{pipeline}")
  @Operation(summary = "runs the workflow", description = "runs the workflow and returns its status and (with callback) the results", tags = Array("run"),
    requestBody = new RequestBody(
      content = Array(new Content(
        mediaType = "application/json",
        schema = new Schema(
          example = """{"quantification.samples": [ "GSM1698568","GSM1698570","GSM2927683","GSM2927750","GSM2042593","GSM2042596"]}"""))), description = "input JSON"),
    parameters = Array(
      new Parameter(name = "pipeline", in = ParameterIn.PATH, required = true,
        example = "quantification", style = ParameterStyle.DEFAULT, allowReserved = true,
        description = "path to the workflow inside pipelines folder (defined by PIPELINES enviroment variable, /data/pipelines by default)"),
      new Parameter(name = "server", in = ParameterIn.QUERY, required = false, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "URL of the cromwell server, enviroment variable CROMWELL by default"),
      new Parameter(name = "authorization", in = ParameterIn.QUERY, required = false, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "Optional authorization parameter"),
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
  def runAPI: Route = pathPrefix("run" / Remaining) { pipeline =>
    debug(s"BEFORE PARAMETER EXTRACTION FOR ${pipeline}")
    extractPipeline(pipeline) match {
      case (None, _, _) =>
        error(s"CANNOT FIND ${pipeline}")
        reject(PipelinesRejections.PipelineNotFound(pipeline))

      case (Some(wdl), deps, defs) =>
        parameters("server".?, "callback".?, "authorization".?) { (serverOpt, callBackOpt, authOpt) =>
          debug(s"FOUND PARAMETERS FOR RUNNING ${pipeline}")
          //val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
          val serverURL = serverOpt.getOrElse(CromwellClient.defaultURL)
          entity(as[String]) { json =>
            val js = concatJson(defs, json)
            debug(js)
            val toRun = Commands.Run(wdl, js, "", deps) //TODO: fix problem
            val headers = authOpt.fold(Map.empty[String, String])(a=>Map("Authorization" -> a))
            val serverMessage = MessagesAPI.ServerCommand(toRun, serverURL, callBackOpt.map(Set(_)).getOrElse(Set.empty[String]),_, headers)
            completeOrRecoverWith((runner ? serverMessage).mapTo[StatusInfo]) { extraction =>
              debug(s"running pipeline failed with ${extraction}")
              failWith(extraction) // not executed.
            }
          }
        } ~ {
          debug("POST REQUEST!")
          debug("WDL FOUND!")
          debug(s"WDL IS: ${wdl}")
          if(deps.nonEmpty) debug(s"Found dependencies [${deps.map(_._1).mkString(", ")}]!")
          complete(HttpResponse(StatusCodes.OK, Nil, wdl))
        }
    }
  }

  def routes: Route = runAPI

}
