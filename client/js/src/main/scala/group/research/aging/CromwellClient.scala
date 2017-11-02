package group.research.aging

import group.research.aging.cromwell.client.CromwellClientShared

object CromwellClient {
  lazy val localhost = new CromwellClient("http://localhost:8000", "v1")
}

class CromwellClient(val base: String,
                     val version: String)
  extends CromwellClientShared with CromwellClientJSspecific {

}