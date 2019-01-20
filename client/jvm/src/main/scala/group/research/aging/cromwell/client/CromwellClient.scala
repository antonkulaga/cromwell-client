package group.research.aging.cromwell.client

import akka.http.scaladsl.HttpExt
import cats.effect.IO
import hammock.akka.AkkaInterpreter
import hammock.apache.ApacheInterpreter
import io.circe.generic.JsonCodec
import akka.stream.ActorMaterializer
import cats.effect.IO
import cats.free.Free

import scala.concurrent.ExecutionContext

object CromwellClient {

  lazy val localhost: CromwellClient = new CromwellClient("http://localhost:8000", "v1")

  lazy val  default: CromwellClient = new CromwellClient(scala.util.Properties.envOrElse("CROMWELL", "http://localhost:8000" ), "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")

}

@JsonCodec case class CromwellClient(base: String, version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific{
  implicit override protected def getInterpreter: ApacheInterpreter[IO] = ApacheInterpreter[IO]
}

case class CromwellClientAkka(base: String, version: String = "v1", http: HttpExt)( implicit val mat: ActorMaterializer, executionContext: ExecutionContext) extends CromwellClientShared {

  import  implicits._

  implicit override protected def getInterpreter: AkkaInterpreter[IO] = new AkkaInterpreter[IO](http)

}