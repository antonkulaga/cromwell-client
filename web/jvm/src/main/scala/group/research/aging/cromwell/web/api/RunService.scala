package group.research.aging.cromwell.web.api

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._
import akka.pattern.ask
import akka.util.Timeout
import group.research.aging.cromwell.client.{CromwellClient, StatusInfo}
import group.research.aging.cromwell.web.Commands.BatchRun
import group.research.aging.cromwell.web.{Commands, Pipeline}
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import group.research.aging.cromwell.web.util.PipelinesExtractor
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.{Explode, ParameterIn, ParameterStyle}
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

@Path("/api")
class RunService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService with PipelinesExtractor {

  @POST
  @Path("/run/{pipeline}")
  @Operation(summary = "runs the workflow", description = "runs the workflow and returns its status and (with callback) the results", tags = Array("run"),
    requestBody = new RequestBody(
      content = Array(new Content(
        mediaType = "application/json",
        schema = new Schema(
          example = """{"quantification.samples": [ "GSM1698568","GSM1698570"]}"""))), description = "input JSON"),
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
      case None =>
        error(s"CANNOT FIND ${pipeline}")
        reject(PipelinesRejections.PipelineNotFound(pipeline))

      case Some(p) =>
        parameters("server".?, "callback".?, "authorization".?) { (serverOpt, callBackOpt, authOpt) =>
          debug(s"FOUND PARAMETERS FOR RUNNING ${pipeline}")
          //val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
          val serverURL = serverOpt.getOrElse(CromwellClient.defaultURL)
          entity(as[String]) { json =>
            val toRun =  p.to_run(json)
            val headers = authOpt.fold(Map.empty[String, String])(a=>Map("Authorization" -> a))
            val cbs = callBackOpt.map(Set(_)).getOrElse(Set.empty[String])
            val serverMessage: MessagesAPI.ServerCommand =
              MessagesAPI.ServerCommand(toRun, serverURL, cbs, false, headers)
            debug(s"sending a message ${serverMessage}")
            completeOrRecoverWith((runner ? serverMessage).mapTo[StatusInfo]) { extraction =>
              debug(s"running pipeline failed with ${extraction}")
              failWith(extraction) // not executed.
            }
          }
        } ~ {
          debug("POST REQUEST!")
          debug("WDL FOUND!")
          debug(s"WDL IS: ${p.main}")
          if(p.dependencies.nonEmpty) debug(s"Found dependencies [${p.dependencies.map(_._1).mkString(", ")}]!")
          complete(HttpResponse(StatusCodes.OK, Nil, p.main))
        }
    }
  }


  @GET
  @Path("/run/batch/{pipeline}")
  @Operation(summary = "batch runs the workflow", description = "batch runs workflow", tags = Array("run"),
    parameters = Array(
      new Parameter(name = "title", in = ParameterIn.QUERY, required = true, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "batch title"),
      new Parameter(name = "pipeline", in = ParameterIn.PATH, required = true,
        example = "quantification", style = ParameterStyle.DEFAULT, allowReserved = true,
        description = "path to the workflow inside pipelines folder (defined by PIPELINES enviroment variable, /data/pipelines by default)"),

      new Parameter(name = "experiments", in = ParameterIn.QUERY, required = true, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "List of come-separated experiments to run"),
      new Parameter(name = "batch_size", in = ParameterIn.QUERY, required = false, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "batch size, 4 by default"),

      new Parameter(name = "servers", in = ParameterIn.QUERY, required = false, style = ParameterStyle.SIMPLE, allowReserved = true,
        description = "URLs of the cromwell servers, coma-separated")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Batch started",
        content = Array(new Content(schema = new Schema(example = """{
  "id": "e442e52a-9de1-47f0-8b4f-e6e565008cf1",
  "status": "Submitted"
}""")))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  def batchRunAPI: Route = pathPrefix("run" / "batch" / Remaining) { pipeline =>
    debug(s"BEFORE PARAMETER EXTRACTION FOR ${pipeline}")
    extractPipeline(pipeline) match {
      case None =>
        error(s"CANNOT FIND ${pipeline}")
        reject(PipelinesRejections.PipelineNotFound(pipeline))

      case Some(p) =>
        parameters("title".?(""),"experiments", "servers", "batch_size".?(4))
        //  parameters("server".?, "callback".?, "authorization".?) { (serverOpt, callBackOpt, authOpt) =>
        {
          (title, experiments, servers, batchSize) =>
          //val batchSize = 4
          //val experiments = List.empty[String]
          //val servers = List.empty[String]
          //(experiments, batchSize, servers) => //, callBackOpt, authOpt) =>
            debug(s"FOUND PARAMETERS FOR RUNNING ${pipeline} with batch size ${batchSize}")
            //val c = serverOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
            val serverURLs: Seq[String] = servers.split(",")// serverOpt.getOrElse(CromwellClient.defaultURL)
            debug(s"EXPERIMENTS:\n ${experiments}")
            val inputs  = experiments.split(",")

            val ins = inputs.sliding(batchSize).zipWithIndex.map{ case (ee, i)=>
                s"""{
                "${pipeline}.${if(pipeline.contains("run")) "runs" else "experiments"}": [${ee.map(e=> "\"" + e + "\"").mkString(", ")}],
                "${pipeline}.title": "${title}_${i}_to_${i+batchSize}"
                }"""
            }.toList

            debug("INPUTS ARE: \n"+ ins.mkString("\n"))
            val b = p.to_run_batch(ins, serverURLs, title)
            debug(s"sending a batch ${title}")


            completeOrRecoverWith((runner ? b).mapTo[BatchRun]) { extraction =>
              debug(s"running pipeline failed with ${extraction}")

              failWith(extraction) // not executed.
            }
        } ~ {
          debug("BATCH GET REQUEST!")
          debug("WDL FOUND!")
          //debug(s"WDL IS: ${p.main}")
          if(p.dependencies.nonEmpty) debug(s"Found dependencies [${p.dependencies.map(_._1).mkString(", ")}]!")
          complete(HttpResponse(StatusCodes.OK, Nil, p.main))
        }
    }
  }

  def routes: Route = batchRunAPI ~ runAPI

}
