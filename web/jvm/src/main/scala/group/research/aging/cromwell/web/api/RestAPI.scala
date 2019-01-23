package group.research.aging.cromwell.web.api

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, Route, StandardRoute}

object RestAPI extends RestAPI
class RestAPI extends Directives {

  def host: String = s"localhost:${scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001").toInt}"

  protected def toSwagger: StandardRoute = {
    val url = s"http://${host}/api-docs/swagger.json"
    val fl = s"lib/swagger-ui/index.html?url=${url}"
    redirect(Uri(fl), StatusCodes.TemporaryRedirect)
  }
  def swagger: Route = path("swagger")  {
    toSwagger
  }

  def routes: Route = swagger ~ SwaggerDocService.routes ~ pathPrefix("api") {
    new GetAllService().routes ~ new WorkflowService().routes ~  new RunService().routes ~ toSwagger
  }
}
