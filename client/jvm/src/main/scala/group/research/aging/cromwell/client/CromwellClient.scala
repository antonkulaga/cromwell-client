package group.research.aging.cromwell.client

object CromwellClient {

  lazy val localhost: CromwellClientLike = new CromwellClientJVM("http://localhost:8000", "v1")

  def apply(base: String): CromwellClientJVM = new CromwellClientJVM(base, "v1")

}

class CromwellClientJVM(val base: String, val version: String = "v1") extends CromwellClientShared with CromwellClientJVMSpecific