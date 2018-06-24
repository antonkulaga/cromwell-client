package group.research.aging.cromwel.client

import cats.effect.IO
import cats.free.Free
import fr.hmil.roshttp.body.JSONBody.JSONObject
import group.research.aging.cromwell.client._
import hammock.marshalling.MarshallC
import hammock.{Decoder, Entity, Hammock, HammockF, HttpF, HttpResponse, Method, Uri}

import scala.concurrent.Future

object CromwellClient {
  lazy val localhost = new CromwellClientJS("http://localhost:8000", "v1")

  def apply(base: String): CromwellClientShared = new CromwellClientJS(base, "v1")
}

class CromwellClientJS(val base: String,
                     val version: String)
  extends CromwellClientShared with CromwellClientJSspecific


class CromwellClientRedirect(val base: String, val version: String, val proxy: String = "")
  extends CromwellClientShared with CromwellClientJSspecific  with CromwellClientLike
{

  override def get(subpath: String, headers: Map[String, String]): Free[HttpF, HttpResponse]  = {
    Hammock.request(Method.GET, Uri.unsafeParse(proxy + "redirect/" + base + subpath), headers)
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
}