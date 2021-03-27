package group.research.aging.cromwell.client

import java.net.URI

import _root_.akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.ws.Message
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import cats.effect.{ContextShift, IO}
import hammock.InterpTrans
import hammock.akka.AkkaInterpreter
import hammock.apache.ApacheInterpreter
import io.circe.generic.JsonCodec
import sttp.client3.{SttpBackend, _}
//import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.{ExecutionContext, Future}

object CromwellClient {

  import scala.concurrent.ExecutionContext.Implicits.global


  lazy val defaultServerPort = "8000"

  lazy val localhost: CromwellClient = new CromwellClient(s"http://localhost:${defaultServerPort}", "v1")

  lazy val defaultURL: String = scala.util.Properties.envOrElse("CROMWELL", s"http://localhost:${defaultServerPort}" )

  lazy val defaultClientPort: String = scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001")

  lazy val defaultHost: String = new URI(defaultURL).getHost

  lazy val  default: CromwellClient = new CromwellClient(defaultURL, "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")

}


@JsonCodec case class CromwellClient(base: String, version: String = "v1") extends CromwellClientShared with PostSttp with CromwellClientJVMSpecific
{
  implicit override protected def getInterpreter: InterpTrans[IO] = ApacheInterpreter.instance

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override implicit def sttpBackend= AsyncHttpClientFutureBackend()
}

/*
case class CromwellClientAkka(base: String, version: String = "v1")(implicit val http: HttpExt, val materializer: ActorMaterializer)
                             extends CromwellClientShared with CromwellClientJVMSpecific with PostSttp {
  implicit val executionContext: ExecutionContext = http.system.dispatcher
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  implicit override protected def getInterpreter: InterpTrans[IO] = AkkaInterpreter.instance[IO]//(http)

  /*
  override implicit def sttpBackend: SttpBackend[Future, Source[ByteString, Any], Flow[Message, Message, *]] =
    AkkaHttpBackend.usingActorSystem(http.system,
      options = SttpBackendOptions.Default
    )
   */
}
*/