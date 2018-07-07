package group.research.aging.cromwell.client

import cats.effect.IO
import cats.free.Free
import fr.hmil.roshttp.body.JSONBody.JSONObject
import group.research.aging.cromwell.client._
import hammock.marshalling.MarshallC
import hammock.{Decoder, Entity, Hammock, HammockF, HttpF, HttpResponse, Method, Uri}
import io.circe.generic.JsonCodec

import scala.concurrent.Future

object CromwellClient {
  lazy val localhost: CromwellClient = new CromwellClient("http://localhost:8000", "v1")

  def apply(base: String): CromwellClient = new CromwellClient(base, "v1")
}

@JsonCodec case class CromwellClient(base: String, version: String)
  extends CromwellClientShared with CromwellClientJSspecific


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