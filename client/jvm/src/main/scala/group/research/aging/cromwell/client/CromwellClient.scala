package group.research.aging.cromwell.client

import io.circe.generic.JsonCodec

import java.net.URI

object CromwellClient {


  lazy val defaultServerPort = "8000"

  lazy val localhost: CromwellClient = new CromwellClient(s"http://localhost:${defaultServerPort}", "v1")

  lazy val defaultURL: String = scala.util.Properties.envOrElse("CROMWELL", s"http://localhost:${defaultServerPort}" )

  lazy val filePrefixURL: Option[String] = scala.util.Properties.envOrNone("FILE_PREFIX_URL")

  lazy val defaultClientPort: String = scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001")

  lazy val defaultHost: String = new URI(defaultURL).getHost

  lazy val  default: CromwellClient = new CromwellClient(defaultURL, "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")

}


@JsonCodec case class CromwellClient(base: String, version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific