package group.research.aging.cromwell.client

import io.circe.generic.JsonCodec

object CromwellClient {

  lazy val localhost: CromwellClient = new CromwellClient("http://localhost:8000", "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")

}

@JsonCodec case class CromwellClient(base: String, version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific