package group.research.aging.cromwell.web.api

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.{Http, HttpExt, model, server}
import akka.stream.ActorMaterializer
import better.files.File
import cats.effect.IO
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.github.swagger.akka.SwaggerHttpService
import de.heikoseeberger.akkahttpcirce._
import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.CromwellClient
import group.research.aging.cromwell.web.communication.WebsocketServer
import hammock.akka.AkkaInterpreter
import io.circe.Json
import io.circe.generic.auto._
import scalacss.DevDefaults._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}
import javax.ws.rs._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Unparsed
import com.github.swagger.akka.SwaggerHttpService
import group.research.aging.cromwell.web.server.WebServer.getFromBrowseableDirectories
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses._
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.parameters.RequestBody

@Path("/api")
class RunService extends CromwellClientService {

  @POST
  @Path("/run")
  @Operation(summary = "runs the workflow", description = "runs the workflow and returns its status",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[Json])))),
    parameters = Array(new Parameter(name = "wdl", in = ParameterIn.PATH, description = "path to the workflow inside pipelines folder")),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Workflow started",
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
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
      parameters("host".?) { hostOpt =>
        val c = hostOpt.map(CromwellClient(_)).getOrElse(CromwellClient.default)
        post {
          entity(as[Json]) { json =>
            val wdl = fl.contentAsString
            //c.postWorkflowStrings(wdl, json.spaces4)
            //debug("JSON RECEIVED!!!")
            debug("WDL FOUND!")
            debug(s"WDL IS ${fl.contentAsString}")
            debug("INPUT JSON SENT:")
            debug(json.spaces4)
            //System.out.println(entity.getContentType())
            complete(json)
          }
        } ~ {
          debug("WDL FOUND!")
          debug(s"WDL IS ${fl.contentAsString}")
          complete(HttpResponse(StatusCodes.OK, Nil, fl.contentAsString))
        }
      }
    }

  }

  def routes: Route = runAPI
}
