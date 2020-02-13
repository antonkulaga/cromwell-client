package group.research.aging.cromwell.web

import akka.actor.{ActorSystem, OneForOneStrategy, SupervisorStrategy}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.{Http, HttpExt, model}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import cats.effect.{ContextShift, IO}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import de.heikoseeberger.akkahttpcirce._
import group.research.aging.cromwell.web.api.RestAPI
import group.research.aging.cromwell.web.communication.WebsocketServer
import hammock.InterpTrans
import hammock.akka.AkkaInterpreter
import io.circe.generic.auto._
import scalacss.DevDefaults._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Unparsed
import scala.concurrent.duration._

/**
  * Cromwell UI webserver
  */
object WebServer extends HttpApp with FailFastCirceSupport with LogSupport {

  // Set the default log formatter
  Logger.setDefaultFormatter(SourceCodeLogFormatter)
  Logger.setDefaultLogLevel(LogLevel.DEBUG)

  lazy val resourcePrefix = "public"


  def mystyles: Route =  path("styles" / "mystyles.css"){
    complete  {
      HttpResponse(  entity = HttpEntity(MediaTypes.`text/css`.withCharset(HttpCharsets.`UTF-8`),  MyStyles.render   ))   }
  }

  def loadResources: Route = pathPrefix(resourcePrefix ~ Slash) {
    getFromResourceDirectory("")
  }

  lazy val webjarsPrefix = "lib"

  def webjars: Route = pathPrefix(webjarsPrefix ~ Slash)  {  getFromResourceDirectory(webjarsPrefix)  }


  def un(str: String): Unparsed = scala.xml.Unparsed(str)

  def index: Route =  pathSingleSlash{ ctx=> ctx.complete {
      <html>
        <head title="Cromwell client UI">
          <script type="text/javascript" src="/lib/jquery/jquery.min.js"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/fomantic-ui/2.7.8/semantic.css"/>
          <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/fomantic-ui/2.7.8/semantic.js"></script>
          <link rel="stylesheet" href="/styles/mystyles.css"/>
          <!-- Include from a free CDN -->
          <script src="https://cdn.rawgit.com/caldwell/renderjson/master/renderjson.js"></script>
        </head>
        <body>
          <main  id="main">

          </main>
          <!--<div id="test"></div>-->
        </body>
        <!--<script src="/public/cromwell-web-fastopt.js" type="text/javascript"></script>-->
        <script src="/public/cromwell-web-opt.js" type="text/javascript"></script>
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

  def browse: Route = pathPrefix("data" / Remaining) { file =>
    val folder = scala.util.Properties.envOrElse("DATA", "/data") + "/" + file
    getFromBrowseableDirectories(folder)
  }

  val decider: Supervision.Decider = {
    case _: ArithmeticException ⇒ Supervision.Resume
    case _                      ⇒ Supervision.Restart
  }


  implicit lazy val http: HttpExt = Http(this.systemReference.get())
  implicit def actorSystem = http.system
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(http.system).withSupervisionStrategy(decider))
  implicit lazy val executionContext: ExecutionContext = http.system.dispatcher
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(executionContext)

  implicit protected def getInterpreter: InterpTrans[IO] = AkkaInterpreter.instance[IO]

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
/*
  import cats.effect.IO
  import cats.free.Free
  import hammock._
  import hammock.marshalling._

  def get(path: String, headers: Map[String, String] = Map.empty): Free[HttpF, HttpResponse] = Hammock.request(Method.GET, Uri.unsafeParse(path), headers)

  def getIO[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T] =
    get(subpath, headers).as[T](D, M).exec[IO]
*/

  lazy val websocketServer: WebsocketServer = new WebsocketServer()

  lazy val restAPI = new RestAPI()

  override def routes: Route = cors(){
    index~
    websocketServer.route ~
    restAPI.routes ~
    webjars ~
    mystyles ~
    assets ~
    browse ~
    loadResources ~
    redirect
  }

}
