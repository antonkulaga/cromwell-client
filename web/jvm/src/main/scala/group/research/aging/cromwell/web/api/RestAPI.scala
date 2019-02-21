package group.research.aging.cromwell.web.api

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, Route, StandardRoute}
import group.research.aging.cromwell.web.api.runners.RunnerManager
import scala.concurrent.duration._
import akka.util._

class RestAPI(http: HttpExt) extends Directives {

  implicit val timeout: Timeout = 10 seconds

  implicit val system = http.system

  def host: String = s"localhost:${scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001").toInt}"

  lazy val runnerManager: ActorRef = system.actorOf(Props(new RunnerManager(http)))

  protected def toSwagger: StandardRoute = {
    val url = s"http://${host}/api-docs/swagger.json"
    val fl = s"lib/swagger-ui/index.html?url=${url}"
    redirect(Uri(fl), StatusCodes.TemporaryRedirect)
  }

  def swagger: Route = path("swagger")  { toSwagger }

  lazy val routes: Route = swagger ~ SwaggerDocService.routes ~ pathPrefix("api") {
      new TestService(runnerManager).routes ~
      new RunService(runnerManager).routes ~
      new GetAllService(runnerManager).routes ~
      new WorkflowService(runnerManager).routes ~
      new TracingService().routes ~
      toSwagger
  }
}
