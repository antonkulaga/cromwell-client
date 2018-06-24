package group.research.aging.cromwell.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import cats.effect.IO
import hammock.jvm.Interpreter
import play.twirl.api.Html
import scalacss.DevDefaults._
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce._


// Server definition
object WebServer extends HttpApp with FailFastCirceSupport{

  lazy val webjarsPrefix = "lib"
  lazy val resourcePrefix = "public"

  implicit  protected def getInterpreter: Interpreter[IO] = Interpreter[IO]

  def mystyles: Route =  path("styles" / "mystyles.css"){
    complete  {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/css`.withCharset(HttpCharsets.`UTF-8`),  MyStyles.render   ))   }
  }

  def loadResources: Route = pathPrefix(resourcePrefix ~ Slash) {
    getFromResourceDirectory("")
  }


  def webjars: Route = pathPrefix(webjarsPrefix ~ Slash)  {  getFromResourceDirectory(webjarsPrefix)  }

  def defaultPage: Option[Html] = {
   None
  }


  def index: Route =  pathSingleSlash{ ctx=>
    ctx.complete {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/html`.withCharset(HttpCharsets.`UTF-8`), html.index(defaultPage).body  ))
    }
  }

  def assets: Route =  pathPrefix("assets" / Remaining) { file =>
    // optionally compresses the response with Gzip or Deflate
    // if the client accepts compressed responses
    encodeResponse {
      getFromResource("public/" + file)
    }
  }

  import cats.effect.IO
  import cats.free.Free
  import hammock._
  import hammock.circe.implicits._
  import hammock.marshalling._
  import io.circe.Json

  def get(path: String, headers: Map[String, String] = Map.empty): Free[HttpF, HttpResponse] = Hammock.request(Method.GET, Uri.unsafeParse(path), headers)

  def getIO[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T] =
    get(subpath, headers).as[T](D, M).exec[IO]


  def redirect: Route = pathPrefix("redirect" / Remaining) {
    url =>
      println("REDIRECT: " + url)
      complete{
        get(url).as[Json].exec[IO].unsafeRunSync()
      }
  }


  def redirect2: Route = pathPrefix("redirect2" / RemainingPath) {
    url =>
      println("REDIRECT2: " + url)

      complete(getIO[String](url.toString()).unsafeRunSync())
  }


  override def routes: Route = index ~ webjars ~ mystyles ~ assets ~ loadResources ~redirect ~redirect2

}
