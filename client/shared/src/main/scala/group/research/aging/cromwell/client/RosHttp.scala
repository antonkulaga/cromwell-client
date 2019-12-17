package group.research.aging.cromwell.client

import fr.hmil.roshttp.{AnyBody, BackendConfig, HttpRequest}
import fr.hmil.roshttp.body.MultiPartBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body._
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.{Decoder, Error}
import io.circe.parser.parse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import monix.execution.Scheduler.Implicits.global


trait RosHttpBase extends {

  def api: String
  def base: String
  def version: String

  private implicit def tryToFuture[T](t: Try[T]): Future[T] = {
    t match{
      case Success(s) => Future.successful(s)
      case Failure(ex) => Future.failed(ex)
    }
  }


  private implicit def eitherErrorToFuture[T, TError<: Error](t: Either[TError, T]): Future[T] = {
    t match{
      case Left(e: Error) => Future.failed(e)
      case Right(s) => Future.successful(s)
    }
  }


  def postRequest(subpath: String)(multipart: MultiPartBody): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + subpath)
      .withCrossDomainCookies(true)
      .withBackendConfig(BackendConfig(maxChunkSize = 24576, internalBufferLength = 384))
    println(s"POST to ${request.longPath}, base is ${base} subpath is ${subpath}")
    request.post(multipart)
  }

  def post[T](subpath: String)(multipart: MultiPartBody)(implicit decoder: Decoder[T]): Future[T] = {
    postRequest(subpath)(multipart).flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def postAPI[T](subpath: String)(multipart: MultiPartBody)(implicit decoder: Decoder[T]): Future[T] =
  {
    post[T](api + subpath)(multipart)(decoder)
  }
}

trait RosHttp extends RosHttpBase with PostAPI {

  /**
    * 400
    * Malformed Input
    * 500
    * Internal Error
    * @param fileContent
    * @param workflowInputs
    * @param workflowOptions
    * @param workflowDependencies
    * @return
    */
  def postWorkflow(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String = "",
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                         ): Future[group.research.aging.cromwell.client.StatusInfo] = {
    val params = ("workflowSource", PlainTextBody(fileContent)) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    val parts = Map[String, BodyPart](params: _*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  protected def prepareInputOptionsDependencies(
                                                 workflowInputs: String,
                                                 workflowOptions: String = "",
                                                 workflowDependencies: Option[java.nio.ByteBuffer] = None
                                               ): List[(String, BodyPart)] = {
    val inputs: List[(String, BodyPart)] = if (workflowInputs == "") Nil else
      List(("workflowInputs", AnyBody(workflowInputs)))
    val options: List[(String, BodyPart)] = if (workflowOptions == "") Nil else
      List(("workflowOptions", AnyBody(workflowOptions)))
    val deps: List[(String, BodyPart)] =
      workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    inputs ++ options ++ deps
  }

  def postWorkflowURL(url: String,
                      workflowInputs: String,
                      workflowOptions: String = "",
                      workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[StatusInfo] = {
    val params = ("workflowUrl", PlainTextBody(url)) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    val parts = Map[String, BodyPart](params: _*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def describeWorkflow(fileContent: String,
              workflowInputs: String,
              workflowOptions: String = "",
              workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[ValidationResult] = {
    //val inputs: List[(String, BodyPart)] = if (workflowInputs == "") Nil else List(("workflowInputs", AnyBody(workflowInputs)))
    val params = ("workflowSource", PlainTextBody(fileContent)) :: prepareInputOptionsDependencies(workflowInputs, workflowOptions, workflowDependencies)
    val parts = Map[String, BodyPart](params: _*)
    postAPI[group.research.aging.cromwell.client.ValidationResult](s"/womtool/${version}/describe")(new MultiPartBody(parts))
  }

}
