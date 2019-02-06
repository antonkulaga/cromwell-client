package group.research.aging.cromwell.web.api

import akka.http.scaladsl.server._
import group.research.aging.cromwell.client
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

@Path("/api")
class WorkflowService extends CromwellClientService {

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
