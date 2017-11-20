package group.research.aging.cromwell.client

object CromwellClient {

  lazy val localhost: CromwellClient = new CromwellClient("http://localhost:8000", "v1")
  def apply(base: String) = new CromwellClient(base, "v1")

}

class CromwellClient(val base: String, val version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific