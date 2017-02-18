package comp.bio.aging.cromwell.client

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.JSONBody.JSONObject

import scala.concurrent.ExecutionContext
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import fr.hmil.roshttp.response.SimpleHttpResponse

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody._
import fr.hmil.roshttp.body.{BodyPart, JSONBody, MultiPartBody, PlainTextBody}
import io.circe.Decoder.Result
import io.circe.generic.JsonCodec
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe._
import io.circe.generic.semiauto._

object CromwellClient {
  lazy val localhost = new CromwellClient("http://localhost:8000/api", "v1")
}

class CromwellClient(base: String, version: String = "v1") {

  private implicit def tryToFuture[T](t: Try[T]): Future[T] = {
    t match{
      case Success(s) => Future.successful(s)
      case Failure(ex) => Future.failed(ex)
    }
  }


  private implicit def eitherErrorToFuture[T, TError<: Error](t: Either[TError, T]): Future[T] = {
    t match{
      case Left(e:Error) => Future.failed(e)
      case Right(s) => Future.successful(s)
    }
  }

  def waitFor[T](fut: Future[T])(implicit atMost: FiniteDuration = 3 seconds): T = {
    Await.result(fut, atMost)
  }

  def getRequest(subpath: String): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + subpath)
    request.send()
  }

  def get[T](subpath: String)(implicit decoder: Decoder[T]): Future[T] = {
    getRequest(subpath).flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def postRequest(subpath: String)(multipart: MultiPartBody): Future[SimpleHttpResponse] = {
    val request = HttpRequest(base + subpath)
    request.post(multipart)
  }

  def post[T](subpath: String)(multipart: MultiPartBody)(implicit decoder: Decoder[T]): Future[T] = {
    postRequest(subpath)(multipart).flatMap{ res=>
      val result = parse(res.body).right.flatMap(json=>json.as[T](decoder))
      eitherErrorToFuture(result)
    }
  }

  def getStats: Future[Stats] = get[Stats](s"/engine/${version}/stats")


  def getVersion: Future[Version] = get[Version](s"/engine/${version}/version")

  def postWorkflow(fileContent: String,
                   workflowInputs: Option[JSONBody] = None,
                   workflowOptions: Option[JSONBody] = None,
                   wdlDependencies: Option[JSONBody] = None
                  ): Future[Status] = {
    val parts = Map[String, BodyPart]("wdlSource" -> PlainTextBody(fileContent)) ++
      workflowInputs.fold(Map.empty[String, BodyPart])(part  => Map("workflowInputs" -> part))
      workflowOptions.fold(Map.empty[String, BodyPart])(part => Map("workflowOptions" -> part))
      workflowOptions.fold(Map.empty[String, BodyPart])(part => Map("wdlDependencies" -> part))
    post[Status](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def getOutputsRequest(id: String): Future[SimpleHttpResponse] = getRequest(s"/workflows/${version}/${id}/outputs")

  def getOutputs(id: String): Future[Outputs] = get[Outputs](s"/workflows/${version}/${id}/outputs")

  def getLogsRequest(id: String): Future[SimpleHttpResponse] = getRequest(s"/workflows/${version}/${id}/logs")

  def getLogs(id: String): Future[Logs] = get[Logs](s"/workflows/${version}/${id}/logs")

}
