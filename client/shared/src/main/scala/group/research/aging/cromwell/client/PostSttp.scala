package group.research.aging.cromwell.client
import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Multipart
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.circe
import io.circe.Decoder
import sttp.client._
import sttp.model.{Part, Uri}

import akka.stream.scaladsl.{FileIO, Flow, Sink, Source, StreamConverters}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import sttp.client._
import sttp.client.circe._
import sttp.client.akkahttp._
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import scala.collection.immutable.Seq

trait PostSttp extends PostAPI {

  implicit def sttpBackend: SttpBackend[Future, Source[ByteString, Any], Flow[Message, Message, *]]
  implicit def executionContext: ExecutionContext

  protected def parseUri(str: String): Uri =     Uri.parse(str).toOption match {
      case Some(value) => value
      case None =>
        println(s"FAILED TO PARSE ${str} fallinb back to just http://cromwell:8000")
        uri"http://cromwell:8000"
  }

  def postRequest[T](subpath: String)(multipart: Seq[Part[BasicRequestBody]])(implicit decoder: Decoder[T]): Future[Response[Either[ResponseError[circe.Error], T]]] = {
    basicRequest
      .post(parseUri(base + subpath))
      .multipartBody(multipart)
      .response(asJson[T])
      .send()
  }

  def post[T](subpath: String)(parts: Seq[Part[BasicRequestBody]])(implicit decoder: Decoder[T]): Future[T] = {
    val resp: Future[Response[Either[ResponseError[circe.Error], T]]] = basicRequest
      .post(parseUri(base + subpath))
      .multipartBody(parts)
      .response(asJson[T])
      .send()
    resp.flatMap{
      resp =>
        resp.body match {
          case Left(error) =>
            println(s"FAILED REQUEST with status ${resp.statusText}")
            Future.failed(error)
          case Right(value) => Future.successful(value)
        }
    }
  }

  def postAPI[T](subpath: String)(parts: Seq[Part[BasicRequestBody]])(implicit decoder: Decoder[T]): Future[T] =
  {
    post[T](api + subpath)(parts)(decoder)
  }

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

  override def postWorkflow(fileContent: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]): Future[StatusInfo] = {
    val parts = multipart("workflowSource", fileContent) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(parts)
  }

  override def postWorkflowURL(url: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]): Future[StatusInfo] = {
    val parts = multipart("workflowUrl", url) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(parts)
  }

  override def describeWorkflow(fileContent: String, workflowInputs: String, workflowOptions: String, workflowDependencies: Option[ByteBuffer]): Future[ValidationResult] = {
    val parts = multipart("workflowSource", fileContent) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    postAPI[group.research.aging.cromwell.client.ValidationResult](s"/womtool/${version}/describe")(parts)
  }
}
