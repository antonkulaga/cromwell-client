package group.research.aging.cromwell.web.api

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, Route, StandardRoute}
import group.research.aging.cromwell.web.api.runners.RunnerManager

import scala.concurrent.duration._
import akka.util._
import com.github.swagger.akka.{CustomMediaTypes, SwaggerHttpService}
import com.github.swagger.akka.SwaggerHttpService.apiDocsBase
import com.github.swagger.akka.model.Info

class RestAPI(http: HttpExt) extends SwaggerHttpService with Directives {

  implicit val timeout: Timeout = 10 seconds

  implicit val system = http.system

  //def clientHost: String = s"localhost:${scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001").toInt}"

  override val apiClasses: Set[Class[_]] = Set( classOf[GetAllService], classOf[WorkflowService], classOf[RunService], classOf[TestService])

  //override val host = s"${CromwellClient.defaultHost}:${CromwellClient.defaultClientPort.toInt}"
  override val info = Info(version = "0.1.1")
  //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")


  lazy val runnerManager: ActorRef = system.actorOf(Props(new RunnerManager(http)))

  //def swagger: Route = path("swagger")  { toSwagger }

  def swagger: Route = {
    //pathPrefix( Slash)  {  getFromResourceDirectory("lib")  } ~
    path("swagger.json") {
      get {
        complete(HttpEntity(MediaTypes.`application/json`, generateSwaggerJson))
      }
    } ~
      path("swagger.yaml") {
        get {
          complete(HttpEntity(CustomMediaTypes.`text/vnd.yaml`, generateSwaggerYaml))
        }
      } ~{
      val res = s"lib/swagger-ui/index.html"
      getFromResourceDirectory(res)
    } ~ pathEnd{extractUri{
        uri=>
        val h = uri.authority.host.address()
        val port = uri.authority.port.toString
        val url = s"http://${h}${if(port!="") ":" + port else ""}/api/swagger.yaml"
        val fl = s"api/index.html?url=${url}"
        redirect(Uri(fl), StatusCodes.TemporaryRedirect)
      }
      //getFromResourceDirectory(s"lib/swagger-ui/index.html?url=${url}")
    } ~ {
      getFromResourceDirectory("lib/swagger-ui/")
    }
  }

  override val routes: Route =  pathPrefix("api") {
      new TestService(runnerManager).routes ~
      new RunService(runnerManager).routes ~
      new GetAllService(runnerManager).routes ~
      new WorkflowService(runnerManager).routes ~
      new TracingService().routes ~
      swagger
  }
}
