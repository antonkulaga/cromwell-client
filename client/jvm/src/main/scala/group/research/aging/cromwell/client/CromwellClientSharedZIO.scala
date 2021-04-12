package group.research.aging.cromwell.client
import zio.{Task, URIO, ZIO}
import sttp.client3._
import sttp.client3.circe._
import io.circe.Decoder
import sttp.model._
import wvlet.log.LogSupport

import java.net.http.HttpClient
import java.nio.ByteBuffer
import scala.collection.immutable.ListMap

trait CromwellClientSharedZIO {
  self: LogSupport =>

  def base: String
  def version: String
  def api = "/api"

  type ResponseExceptionJson = ResponseException[String, io.circe.Error]
  type JsonRequest[T] = RequestT[Identity, Either[ResponseExceptionJson, T], Any]

  protected def parseUri(str: String): Uri


  /**
    * Send requests that assumes String answer, i.e. simple get or post without body
    * @param request
    * @return
    */
  def text_request_zio(request: Request[Either[String, String], Any] ): ZIO[Any, Throwable, Response[Right[HttpError[String], String]]]

  def json_request_zio[T](request: JsonRequest[T])(implicit decoder: io.circe.Decoder[T]): ZIO[Any, Throwable, T]

  // IMPLEMENTATIONS

  /*
  Get Request done with zio
   */
  def get_zio(subpath: String): ZIO[Any, Throwable, Response[Right[HttpError[String], String]]] = text_request_zio(basicRequest.get(parseUri(base + subpath)))


  def get_json_zio[T](subpath: String)(implicit decoder: io.circe.Decoder[T]): ZIO[Any, Throwable, T] = {
    val request:  JsonRequest[T] = basicRequest.get(parseUri(base + subpath)).response(asJson[T])
    json_request_zio(request)(decoder)
  }

  def get_api_json_zio[T](subpath: String)(implicit decoder: io.circe.Decoder[T]): ZIO[Any, Throwable, T] = {
    this.get_json_zio[T](api + subpath)(decoder)
  }

  def postSimpleZIO[TResponse](subpath: String)(implicit decoder: io.circe.Decoder[TResponse]): ZIO[Any, Throwable, TResponse] = {
    val request =  basicRequest
      .post(parseUri(base + subpath))
      .response(asJson[TResponse])
    json_request_zio(request)(decoder)
  }

  def post_simple_api_zio[TResponse](subpath: String)(implicit decoder: io.circe.Decoder[TResponse]): ZIO[Any, Throwable, TResponse] = {
    this.postSimpleZIO(api + subpath)(decoder)
  }

  def postZIO[TBody, TResponse](subpath: String, body: TBody)(implicit serializer: BodySerializer[TBody], decoder: io.circe.Decoder[TResponse]) = {
    val request = basicRequest
      .post(parseUri(base + subpath))
      .body[TBody](body)(serializer)
      .response(asJson[TResponse])
    this.json_request_zio(request)(decoder)
  }


  def postMultipartZIO[TResponse](subpath: String, parts: Seq[Part[BasicRequestBody]])(implicit decoder: io.circe.Decoder[TResponse]) = {
    val request = basicRequest
      .post(parseUri(base + subpath))
      .multipartBody(parts)
      .response(asJson[TResponse])
    this.json_request_zio(request)(decoder)
  }

  def post_api_multipart_zio[T](subpath: String)(parts: Seq[Part[BasicRequestBody]])(implicit decoder: Decoder[T]) =
    postMultipartZIO[T](api + subpath, parts)(decoder)

  protected def prepareInputOptionsDependencies(
                                                 workflowInputs: String,
                                                 workflowOptions: String = "",
                                                 workflowDependencies: Option[java.nio.ByteBuffer] = None
                                               ): List[Part[BasicRequestBody]] = {
    val inputs: List[Part[BasicRequestBody]] = if (workflowInputs == "") List.empty[Part[BasicRequestBody]] else
      multipart("workflowInputs", workflowInputs)::Nil
    val options: List[Part[BasicRequestBody]] = if (workflowOptions == "") List.empty[Part[BasicRequestBody]] else
      multipart("workflowOptions", workflowOptions)::Nil
    val deps: List[Part[BasicRequestBody]] =
      workflowDependencies.fold(List.empty[Part[BasicRequestBody]])(part  => List(multipart("workflowDependencies", ByteBufferBody(part))))
    inputs ++ options ++ deps
  }

  def postWorkflow(fileContent: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]) = {
    println("***********POSTING WORKFLOW*************************")
    val parts = multipart("workflowSource", fileContent) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    post_api_multipart_zio[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(parts)
  }

  def postWorkflowURL(url: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]) = {
    val parts = multipart("workflowUrl", url) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    post_api_multipart_zio[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(parts)
  }

  def describeWorkflow(fileContent: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]) = {
    val parts = multipart("workflowSource", fileContent) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    post_api_multipart_zio[group.research.aging.cromwell.client.ValidationResult](s"/womtool/${version}/describe")(parts)
  }


  def post_api_zio[TBody, TResponse](subpath: String, body: TBody)(implicit serializer: BodySerializer[TBody], decoder: io.circe.Decoder[TResponse]): ZIO[Any, Throwable, TResponse] = {
    this.postZIO(api + subpath, body)(serializer, decoder)
  }


  def getEngineZIO[T](subpath: String)(implicit decoder: io.circe.Decoder[T]): ZIO[Any, Throwable, T] = get_json_zio(s"/engine/${version}" + subpath)

  def getStatsZIO: ZIO[Any, Throwable, Stats] = getEngineZIO[Stats](s"/stats")

  def getVersionZIO: ZIO[Any, Throwable, Version] = getEngineZIO[Version](s"/version")

  def getOutputZIO(id: String): ZIO[Any, Throwable, CallOutputs] = get_api_json_zio[CallOutputs](s"/workflows/${version}/${id}/outputs")

  def getLabelsZIO(id: String): ZIO[Any, Throwable, WorkflowLabels] = get_api_json_zio[WorkflowLabels](s"/workflows/${version}/${id}/labels")

  protected def queryString(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): String = status match {
    case WorkflowStatus.AnyStatus => s"/workflows/${version}/query?includeSubworkflows=${includeSubworkflows}"
    case status: WorkflowStatus =>   s"/workflows/${version}/query?status=${status.entryName}&includeSubworkflows=${includeSubworkflows}"
  }

  def getQueryZIO(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): ZIO[Any, Throwable, QueryResults] = {
    val url = queryString(status, includeSubworkflows)
    get_api_json_zio[QueryResults](url)
  }

  /**
    * 400
    * Malformed Workflow ID
    * 403
    * Workflow in terminal status
    * 404
    * Workflow ID Not Found
    * 500
    * Internal Error
    */
  def abortZIO(id: String): ZIO[Any, Throwable, StatusInfo] =
  {
    logger.debug(s"abborting ${id}")
    this.post_simple_api_zio[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}/${id}/abort")
  }



  def getLogsZIO(id: String): ZIO[Any, Throwable, Logs] = this.get_api_json_zio[Logs](s"/workflows/${version}/${id}/logs")
  def getStatusZIO(id: String): ZIO[Any, Throwable, StatusInfo] = this.get_api_json_zio[StatusInfo](s"/workflows/${version}/${id}/logs")
  def getBackendsZIO: ZIO[Any, Throwable, Backends] = this.get_api_json_zio[Backends](s"/workflows/${version}/backends")

  def getMetadataZIO(id: String, v: String = "v2", expandSubWorkflows: Boolean = true): ZIO[Any, Throwable, Metadata] =
    this.get_api_json_zio[Metadata](s"/workflows/${v}/${id}/metadata?expandSubWorkflows=${expandSubWorkflows}")

  def getAllOutputsZIO(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): ZIO[Any, Throwable, List[CallOutputs]] = {
    val query: ZIO[Any, Throwable, QueryResults] = getQueryZIO(status, includeSubworkflows)
    query.map{ case result =>
      result.results.map(r=>this.getOutputZIO(r.id))
    }.flatMap{ case results => ZIO.collectAllSuccessesPar(results) }
  }

  def getAllLogsZIO(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): ZIO[Any, Throwable, List[Logs]] = {
    val query: ZIO[Any, Throwable, QueryResults] = getQueryZIO(status, includeSubworkflows)
    query.map{ case result =>
      result.results.map(r=>this.getLogsZIO(r.id))
    }.flatMap{ case results => ZIO.collectAllSuccessesPar(results) }
  }

  def getAllMetadataZIO(status: WorkflowStatus = WorkflowStatus.AnyStatus, includeSubworkflows: Boolean = false): ZIO[Any, Throwable, List[Metadata]] = {
    val query: ZIO[Any, Throwable, QueryResults] = getQueryZIO(status, includeSubworkflows)
    query.map{ case result =>
      result.results.map(r=>this.getMetadataZIO(r.id))
    }.flatMap{ case results => ZIO.collectAllSuccessesPar(results) }
  }

}
