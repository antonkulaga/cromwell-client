package group.research.aging.cromwell.client

import java.net.URI

import _root_.akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer
import cats.effect.{ContextShift, IO}
import hammock.InterpTrans
import hammock.akka.AkkaInterpreter
import hammock.apache.ApacheInterpreter
import io.circe.generic.JsonCodec

import scala.concurrent.ExecutionContext

object CromwellClient {


  lazy val defaultServerPort = "8000"

  lazy val localhost: CromwellClient = new CromwellClient(s"http://localhost:${defaultServerPort}", "v1")

  lazy val defaultURL: String = scala.util.Properties.envOrElse("CROMWELL", s"http://localhost:${defaultServerPort}" )

  lazy val defaultClientPort: String = scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001")

  lazy val defaultHost: String = new URI(defaultURL).getHost

  lazy val  default: CromwellClient = new CromwellClient(defaultURL, "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")

}

@JsonCodec case class CromwellClient(base: String, version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific{
  implicit override protected def getInterpreter: InterpTrans[IO] = ApacheInterpreter.instance
}

case class CromwellClientAkka(base: String, version: String = "v1")(implicit val http: HttpExt, val materializer: ActorMaterializer)
                             extends CromwellClientShared with CromwellClientJVMSpecific{
  implicit val executionContext: ExecutionContext = http.system.dispatcher
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  implicit override protected def getInterpreter: InterpTrans[IO] = AkkaInterpreter.instance[IO]//(http)

}