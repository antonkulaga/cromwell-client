package group.research.aging.cromwell.client

import io.circe.generic.JsonCodec

object CromwellClient {
  lazy val localhost: CromwellClient = new CromwellClient("http://localhost:8000", "v1")

  lazy val defaultClientPort: String = "8001"

  lazy val defaultHost: String = "localhost"

  def apply(base: String): CromwellClient = {
    val url = if(base.endsWith("/")) {
      println("deleting last / symbol")
      base.take(base.length-1)
    } else base
    new CromwellClient(url, "v1")
  }
}

@JsonCodec case class CromwellClient(base: String, version: String) extends CromwellClientLike
//CromwellClientShared
with CromwellClientJSspecific
{
  lazy val api = "/api"
}
/*
  def postWorkflowStrings(fileContent: String,
                          workflowInputs: String,
                          workflowOptions: String,
                          workflowDependencies: Option[java.nio.ByteBuffer] = None
                         ): Future[group.research.aging.cromwell.client.StatusInfo]

  /**
    * 400
    * Malformed Input
    * 500
    * Internal Error
    * @param fileContent
    * @param workflowInputs
    * @param workflowOptions
    * @param workflowDependencies
    * @return
    */
  def postWorkflow(fileContent: String,
                   workflowInputs: Option[JSONObject] = None,
                   workflowOptions: Option[JSONObject] = None,
                   workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[group.research.aging.cromwell.client.StatusInfo]
                   */
