package group.research.aging.cromwell.web.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.{CallOutputs, CromwellClient, WorkflowStatus}
import group.research.aging.cromwell.web.{Commands, Results}
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

import scala.concurrent.Future
import scala.concurrent.duration._

@Path("/api")
class WorkflowService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService {


  @GET
  @Path("/metadata")
  @Operation(summary = "Return metadata", description = "Return metadata for specific pipeline",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the metadata"),
      new Parameter(name = "server", in = ParameterIn.QUERY, description = "url to the cromwell server"),
      new Parameter(name = "subworkflows", in = ParameterIn.QUERY, description = "if subworkflows should be shown")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Metadata for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.Metadata])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def metaData: Route = pathPrefix("metadata" / Remaining) { id =>
    withServerExtended("gettings metadata"){
      (server, status, incl) =>
        val comm = MessagesAPI.ServerCommand(Commands.GetAllMetadata(status, incl), server)
       (runner ? comm).mapTo[Results.UpdatedMetadata]
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
    withServer("getting outputs"){
      server =>
        val g = Commands.SingleWorkflow.GetOutput(id)
        val comm = MessagesAPI.ServerCommand(g, server)
        val fut = (runner ? comm).mapTo[CallOutputs]
        fut
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
    withServer("getting status"){
      server =>
        val g = Commands.SingleWorkflow.GetStatus(id)
        val comm = MessagesAPI.ServerCommand(g, server)
        val fut = (runner ? comm).mapTo[CallOutputs]
        fut
    }
  }

  def routes: Route = metaData ~ outputs ~ status

}
