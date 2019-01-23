package group.research.aging.cromwell.web.api


import akka.http.scaladsl.server._
import group.research.aging.cromwell.client
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

@Path("/api")
class GetAllService extends CromwellClientService {
  @GET
  @Path("/all")
  @Operation(summary = "Return all metadata", description = "Return all metadata",
    parameters = Array(
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server"),
      new Parameter(name = "status", in = ParameterIn.QUERY, description = "show only workflows with some status"),
      new Parameter(name = "subworkflows", in = ParameterIn.QUERY, description = "if subworkflows should be shown")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Metadata for all pipelines",
        content = Array(new Content(schema = new Schema(implementation = classOf[List[client.Metadata]])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def getAllMeta: Route =
    withCromwell { (c, status, sub) =>
      val meta = c.getAllMetadata(status, sub)
      import io.circe.syntax._
      meta.unsafeRunSync().asJson
    }



  def routes: Route  = pathPrefix("all"){ getAllMeta }
}
