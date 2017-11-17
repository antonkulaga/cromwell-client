package group.research.aging.cromwel.client

import group.research.aging.cromwell.client.CromwellClientShared

object CromwellClient {
  lazy val localhost = new CromwellClient("http://localhost:8000", "v1")
  def apply(base: String) = new CromwellClient(base, "v1")
}

class CromwellClient(val base: String,
                     val version: String)
  extends CromwellClientShared with CromwellClientJSspecific {

}