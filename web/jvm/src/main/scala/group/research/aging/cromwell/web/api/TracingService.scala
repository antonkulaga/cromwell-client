package group.research.aging.cromwell.web.api

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.server._
import group.research.aging.cromwell.client
import io.circe.Json
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

@Path("/api")
class TracingService extends BasicService {

  @Path("/trace")
  def traceAny: Route = pathPrefix("trace") {  entity(as[String]) { json => ctx =>
      debug("RECEIVED REQUEST WITH HEADERS: " + ctx.request.headers)
      debug("RECEIVED JSON: ")
      debug("RECEIVED JSON STRING: ")
     debug(json)
      ctx.complete(json)
    } ~ entity(as[Json]) { json => ctx =>
      debug("RECEIVED REQUEST WITH HEADERS: " + ctx.request.headers)
      debug("RECEIVED JSON: ")
      debug(json)
      ctx.complete(json)
  } ~ { ctx=>
    val st = ctx.request.toString()
    debug(s"tracing GET request: ${st}")
    debug(s"request headers are: ${ctx.request.headers}")
    ctx.complete(st)
  }
  }

  def routes: Route  = traceAny
}
