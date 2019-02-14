package group.research.aging.cromwell.web.api

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server._
import com.github.swagger.akka.SwaggerHttpService
import group.research.aging.cromwell.client.CromwellClient
import wvlet.log.LogSupport

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set( classOf[GetAllService], classOf[WorkflowService], classOf[RunService])

  CromwellClient.defaultURL
  override val host = s"${CromwellClient.defaultHost}:${CromwellClient.defaultClientPort.toInt}"
  //override val info = Info(version = "1.0")
  //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
  //override val info = Info(version = "3.0")
  //override val externalDocs = Some(new ExternalDocumentation().description("Core Docs").url("http://acme.com/docs"))
  //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
  //override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
}