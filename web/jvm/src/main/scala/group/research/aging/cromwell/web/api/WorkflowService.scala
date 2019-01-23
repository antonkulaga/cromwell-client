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

@Path("/api")
class WorkflowService extends CromwellClientService {

  @GET
  @Path("/metadata")
  @Operation(summary = "Return metadata", description = "Return metadata for specific pipeline",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the metadata"),
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server"),
      new Parameter(name = "subworkflows", in = ParameterIn.QUERY, description = "if subworkflows should be shown")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Metadata for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.Metadata])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def metaData: Route = pathPrefix("metadata" / Remaining) { id =>
    withCromwell { (c, _, sub) =>
      val meta = c.getMetadata(id, expandSubWorkflows = sub)
      import io.circe.syntax._
      meta.unsafeRunSync().asJson
    }
  }

  @GET
  @Path("/outputs")
  @Operation(summary = "Return outputs", description = "Return outputs of specific workflow",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the metadata"),
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Outputs for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.CallOutputs])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def outputs: Route =  pathPrefix("outputs" / Remaining) { id =>
    withCromwell { c =>
      val outputs = c.getOutputs(id)
      import io.circe.syntax._
      outputs.unsafeRunSync().asJson
    }
  }
  @GET
  @Path("/status")
  @Operation(summary = "Return status", description = "Return status of specific workflow",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the status"),
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Outputs for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.StatusInfo])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def status: Route =   pathPrefix("status" / Remaining) { id =>
    withCromwell { c =>
      val result = c.getStatus(id)
      import io.circe.syntax._
      c.getEngineStatus
      result.unsafeRunSync().asJson
    }
  }

  def routes: Route = metaData ~ outputs ~ status

}
