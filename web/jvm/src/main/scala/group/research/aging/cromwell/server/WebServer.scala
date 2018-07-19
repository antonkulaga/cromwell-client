package group.research.aging.cromwell.server

import akka.actor.Status.Success
import akka.http.scaladsl.model.StatusCodes.Redirection
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route, RouteResult}
import cats.effect.IO
import hammock.jvm.Interpreter
import scalacss.DevDefaults._
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.{Http, HttpExt, model}
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.headers.{Host, HttpOriginRange, RawHeader}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Unparsed
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

import scala.concurrent.Future

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


  def un(str: String): Unparsed = scala.xml.Unparsed(str)

  def index: Route =  pathSingleSlash{ ctx=> ctx.complete {
      <html>
        <head title="Cromwell client UI">
          <script type="text/javascript" src="/lib/jquery/jquery.min.js"></script>
          <link rel="stylesheet" href="/lib/Semantic-UI/semantic.css"/>
          <script type="text/javascript" src="/lib/Semantic-UI/semantic.js"></script>
          <link rel="stylesheet" href="/styles/mystyles.css"/>
        </head>
        <body id="main">

        </body>
        <script src="/public/cromwell-web-fastopt.js" type="text/javascript"></script>
      </html>
    }
  }

  def assets: Route =  pathPrefix("assets" / Remaining) { file =>
    // optionally compresses the response with Gzip or Deflate
    // if the client accepts compressed responses
    encodeResponse {
      getFromResource("public/" + file)
    }
  }
  implicit lazy val http: HttpExt = Http(this.systemReference.get())

  def proxy(request: HttpRequest, url: model.Uri): Future[RouteResult] = {
    println("url to send is: " + url.toString())
    val proxyRequest = HttpRequest(uri = url,
      headers = Nil,
      entity = request.entity,
      method = request.method)
    http.singleRequest(proxyRequest).map{ p=>
      val r  = p.addHeader(headers.`Access-Control-Allow-Origin`.*)
      RouteResult.Complete(r)
    }
  }
  def redirect: Route =  {
    pathPrefix("http" ~ Remaining) {
      u => ctx =>
        val uri  = model.Uri("http" + u)
        proxy(ctx.request, uri)
        //redirect(url, StatusCodes.TemporaryRedirect)
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


  override def routes: Route = cors(){index ~ webjars ~ mystyles ~ assets ~ loadResources ~redirect}

}
