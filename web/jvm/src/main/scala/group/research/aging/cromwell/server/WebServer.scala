package group.research.aging.cromwell.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import html._
import play.twirl.api.Html
import scalacss.DevDefaults._

// Server definition
object WebServer extends HttpApp {


  lazy val webjarsPrefix = "lib"
  lazy val resourcePrefix = "resources"

  def mystyles =    path("styles" / "mystyles.css"){
    complete  {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/css`.withCharset(HttpCharsets.`UTF-8`),  MyStyles.render   ))   }
  }

  def loadResources = pathPrefix(resourcePrefix ~ Slash) {
    getFromResourceDirectory("")
  }


  def webjars =pathPrefix(webjarsPrefix ~ Slash)  {  getFromResourceDirectory(webjarsPrefix)  }

  def defaultPage: Option[Html] = {
   Some(html.cromwell())
  }


  def index =  pathSingleSlash{ctx=>
    ctx.materializer.executionContext
    ctx.complete {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), html.index(defaultPage).body  ))
    }
  }

  def assets =  pathPrefix("assets" / Remaining) { file =>
    // optionally compresses the response with Gzip or Deflate
    // if the client accepts compressed responses
    encodeResponse {
      getFromResource("public/" + file)
    }
  }

  override def routes: Route = index ~ webjars ~ mystyles ~ assets ~ loadResources

}