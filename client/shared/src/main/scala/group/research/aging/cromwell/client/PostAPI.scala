package group.research.aging.cromwell.client


import fr.hmil.roshttp.AnyBody
import fr.hmil.roshttp.body._

import scala.concurrent.Future

trait PostAPI {


  def api: String
  def base: String
  def version: String



  def postWorkflow(fileContent: String,
                   workflowInputs: String,
                   workflowOptions: String = "",
                   workflowDependencies: Option[java.nio.ByteBuffer] = None
                  ): Future[group.research.aging.cromwell.client.StatusInfo]

  def postWorkflowURL(url: String,
                      workflowInputs: String,
                      workflowOptions: String = "",
                      workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[StatusInfo]

  def describeWorkflow(fileContent: String,
                       workflowInputs: String,
                       workflowOptions: String = "",
                       workflowDependencies: Option[java.nio.ByteBuffer] = None): Future[ValidationResult]

}