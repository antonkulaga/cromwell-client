package comp.bio.aging.cromwell.client
import java.nio.ByteBuffer

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

  def postWorkflowFiles(file: File): Future[Status] ={
    self.postWorkflow(file.lines.mkString("\n"), None, None, None)
  }

  protected def zipFolder(file: File) = {
    /*
    val dir = File.newTemporaryDirectory()
    val child = dir.createChild(file.name, true).createDirectory()
    val zp = dir.createChild(s"${file.name}.zip", false)
    child.zipTo(zp)
    child.zip()
    */
    file.zip()
  }

  def postWorkflowFiles(file: File,
                        workflowInputs: File,
                        wdlDependencies: File): Future[Status] ={
    self.postWorkflowStrings(
      file.lines.mkString("\n"),
      if(workflowInputs.exists) workflowInputs.lines.mkString("\n") else "",
      "",
      Some(ByteBuffer.wrap(zipFolder(wdlDependencies).loadBytes))
    )
  }

  def postWorkflowFiles(file: File,
                        workflowInputs: File,
                        workflowOptions: File,
                        wdlDependencies: File): Future[Status] ={
    self.postWorkflowStrings(
      file.lines.mkString("\n"),
      if(workflowInputs.exists) workflowInputs.lines.mkString("\n") else "",
      if(workflowOptions.exists) workflowOptions.lines.mkString("\n") else "",
      Some(ByteBuffer.wrap(zipFolder(wdlDependencies).loadBytes))
    )
  }


  def postWorkflowFiles(fileContent: File,
                   workflowInputs: File
                  ): Future[Status] = {
    val wdl = fileContent.lines.mkString("\n")
    val inputs = if(workflowInputs.exists) workflowInputs.lines.mkString("\n") else ""
    self.postWorkflowStrings(wdl, inputs, "")
  }

}
