package comp.bio.aging.cromwell.client
import fr.hmil.roshttp.body.{BodyPart, JSONBody, MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.body.JSONBody.JSONObject

import scala.concurrent.Future

/**
  * Created by antonkulaga on 2/18/17.
  */
trait CromwellClientJVMSpecific {
  self: CromwellClientShared =>

  import java.io.{File => JFile}
  import better.files._

  def postWorkflowFiles(file: File, workflowInputs: Option[JSONObject] = None,
                      workflowOptions: Option[JSONObject] = None,
                      wdlDependencies: Option[JSONObject] = None): Future[Status] =
    self.postWorkflow(file.lines.mkString("\n"), workflowInputs, workflowOptions, wdlDependencies)

  def postWorkflowFiles(fileContent: File,
                   workflowInputs: File
                  ): Future[Status] = {
    val wdl = fileContent.lines.mkString("\n")
    val inputs = workflowInputs.lines.mkString("\n")
    println(s"wdl\n ${wdl}")
    println("==================")
    println("inputs")
    println(inputs)
    self.postWorkflowStrings(wdl, inputs)
  }

}
