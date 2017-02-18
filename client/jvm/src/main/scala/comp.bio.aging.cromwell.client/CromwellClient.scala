package comp.bio.aging.cromwell.client

object CromwellClient {
  lazy val localhost = new CromwellClient("http://localhost:8000/api", "v1")
}

class CromwellClient(val base: String, val version: String) extends CromwellClientShared with CromwellClientJVMSpecific{

}
