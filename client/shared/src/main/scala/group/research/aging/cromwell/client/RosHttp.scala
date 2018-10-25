package group.research.aging.cromwell.client

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.MultiPartBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody.JSONObject
import fr.hmil.roshttp.body._
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.{AnyBody, HttpRequest}
import io.circe.{Decoder, Error}
import io.circe.parser.parse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import monix.execution.Scheduler.Implicits.global

trait RosHttp {

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
    println("POSTING:\n" + base + subpath)
    request.post(multipart).onComplete{
      case Success(value) =>
        println("IT WOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOORKS!")
        println(value)
      case Failure(exception) =>
        println("IT FFFFFFFFFFFFFFFFFFAAAAAAAAAAAAAAIIIIIIIIIIIIIIIILLLLLLLLLLLLEEEEEEEEEEED!")
        println(exception)
        println(exception.getMessage)
    }
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


  /*
  def postIO[A: Codec[A]](subpath: String, headers: Map[String, String], value: A): IO[HttpResponse] =
    Hammock.request[A](Method.POST, Uri.unsafeParse(base + subpath), headers, Some(value)).exec[IO]

  def postAPI[A: Codec[A]](subpath: String, headers: Map[String, String], value: A): IO[HttpResponse] = postIO(api + subpath, headers, value)
*/
  /*
def postWorkflow2(fileContent: String,
                 workflowInputs: Option[String] = None,
                 workflowOptions: Option[String] = None
                ) = {
  val params = ("workflowSource" -> fileContent) ::
    workflowInputs.fold(List.empty[(String, String)])(part => List("workflowInputs" -> part)) ++
      workflowOptions.fold(List.empty[(String, String)])(part => List("workflowOptions" -> part))

  //postAPI(s"/workflows/${version}")(new MultiPartBody(parts))
  postAPI(s"/workflows/${version}", Map.empty, params)
}
*/



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
                   workflowInputs: Option[JSONObject] = None,
                   workflowOptions: Option[JSONObject] = None,
                   workflowDependencies: Option[java.nio.ByteBuffer] = None
                  ): Future[group.research.aging.cromwell.client.StatusInfo] = {
    val params: List[(String, BodyPart)] =
      ("workflowSource" -> PlainTextBody(fileContent)) ::
        workflowInputs.fold(List.empty[(String, BodyPart)])(part  => List("workflowInputs" -> part)) ++
          workflowOptions.fold(List.empty[(String, BodyPart)])(part  => List("workflowOptions" -> part)) ++
          workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val parts = Map[String, BodyPart](params:_*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }

  def postWorkflowStrings(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String,
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                         ): Future[group.research.aging.cromwell.client.StatusInfo] = {
    val inputs: List[(String, BodyPart)] = if (workflowInputs == "") Nil else
      List(("workflowInputs", AnyBody(workflowInputs)))
    val options: List[(String, BodyPart)] = if (workflowOptions == "") Nil else
      List(("workflowOptions", AnyBody(workflowOptions)))
    val deps: List[(String, BodyPart)] =
      workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    val params = ("workflowSource", PlainTextBody(fileContent)) :: inputs ++ options ++ deps
    val parts = Map[String, BodyPart](params: _*)
    postAPI[group.research.aging.cromwell.client.StatusInfo](s"/workflows/${version}")(new MultiPartBody(parts))
  }

}
