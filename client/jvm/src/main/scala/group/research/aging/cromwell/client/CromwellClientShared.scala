package group.research.aging.cromwell.client

import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Header
import wvlet.log.LogSupport
import zio.Task
import sttp.client3._
import sttp.model.Uri
import java.net.URI


trait CromwellClientShared extends CromwellClientLike with CromwellClientSharedZIO with LogSupport {

  def base: String
  def version: String


  protected def parseUri(str: String): Uri =     Uri.parse(str).toOption match { //TODO: ugly fix
    case Some(value) => value
    case None =>
      error(s"FAILED TO PARSE ${str} falling back to just http://cromwell:8000")
      uri"http://cromwell:8000"
  }

  implicit val zioBackend: Task[SttpBackend[Task, ZioStreams with capabilities.WebSockets]] =  HttpClientZioBackend()

  override def baseHost: String = new URI(base).getHost

  lazy val baseNoPort: String = {
    val first = base.indexOf(":/")
    val last = base.lastIndexOf(":")
    if(first!=last) base.substring(0, last) else base
  }

  import sttp.client3._
  import zio._





  //def get(subpath: String, headers: Map[String, String]): Free[HttpF, HttpResponse]

  //def getIO[T](subpath: String, headers: Map[String, String])(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T]

  //def getAPI[T](subpath: String, headers: Map[String, String] = Map.empty)(implicit D: Decoder[T], M: MarshallC[HammockF]): IO[T]

  //def getStats: IO[Stats]

  //def getVersion: IO[Version]

  //def getEngineStatus: IO[Entity]


  def makeWorkflowOptions(output: String, log: String="", call_log: String = ""): String =
    s"""
      {
       "write_to_cache": true,
       "read_from_cache": true,
       "final_workflow_CallOutputs_dir": "${output}",
       ${if(log!="") s""" "final_workflow_log_dir": "${log}", """ else ""}
       ${if (call_log != "") s""" "final_call_logs_dir": "${call_log}", """ else ""}
      }
    """.stripMargin


  /**
    * Send requests that assumes String answer, i.e. simple get or post without body
    * @param request
    * @return
    */
  def text_request_zio(request: Request[Either[String, String], Any] ): ZIO[Any, Throwable, Response[Right[HttpError[String], String]]] = this.zioBackend.flatMap{
    backend =>
      request.send(backend).map{
        case Response(Left(error), code, statusText, _ ,_, _) => Left(HttpError((error, statusText), code))
        case Response( Right(result), code, statusText, headers, history, request) => Right(Response(Right.apply[HttpError[String], String](result), code, statusText, headers, history, request))
      }.absolve
  }

  def just_post_zio[TBody](fullpath: String, body: TBody, headers: Seq[Header] = Seq.empty)(implicit serializer: BodySerializer[TBody]): ZIO[Any, Throwable, Response[Right[HttpError[String], String]]] = {
    val request = basicRequest
      .post(parseUri(base + fullpath))
      .body[TBody](body)(serializer)
      .headers(headers:_*)
    text_request_zio(request)
  }


  def json_request_zio[T](request: JsonRequest[T])(implicit decoder: io.circe.Decoder[T]): ZIO[Any, Throwable, T] = {
    this.zioBackend.flatMap { case backend =>
      val response: Task[Response[Either[ResponseExceptionJson, T]]] = request.send(backend)
      response.map {
        case  Response(Left(error), code, status, header, history, request) =>
          logger.error(s"request with ${request.uri.toString} failed with ${code} CODE and ${error} ERROR and ${status} STATUS")
          Left(error)
        case Response(Right(result), _, _, _, _, _) =>
          Right(result)
      }.absolve
    }
  }


  def runtime = Runtime.default

}