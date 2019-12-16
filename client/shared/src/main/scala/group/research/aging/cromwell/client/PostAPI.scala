package group.research.aging.cromwell.client


import fr.hmil.roshttp.AnyBody
import fr.hmil.roshttp.body._

import scala.concurrent.Future

trait PostAPI {


  def api: String
  def base: String
  def version: String

  protected def prepareInputOptionsDependencies(
                                                 workflowInputs: String,
                                                 workflowOptions: String = "",
                                                 workflowDependencies: Option[java.nio.ByteBuffer] = None
                                               ): List[(String, BodyPart)] = {
    val inputs: List[(String, BodyPart)] = if (workflowInputs == "") Nil else
      List(("workflowInputs", AnyBody(workflowInputs)))
    val options: List[(String, BodyPart)] = if (workflowOptions == "") Nil else
      List(("workflowOptions", AnyBody(workflowOptions)))
    val deps: List[(String, BodyPart)] =
      workflowDependencies.fold(List.empty[(String, BodyPart)])(part  => List("workflowDependencies" -> ByteBufferBody(part)))
    inputs ++ options ++ deps
  }

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