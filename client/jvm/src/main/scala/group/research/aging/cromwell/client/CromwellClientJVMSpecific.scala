package group.research.aging.cromwell.client

import java.io.{File => JFile}
import java.nio.ByteBuffer

import better.files._
import cats.effect.IO

import scala.concurrent.Future

/**
  * Created by antonkulaga on 2/18/17.
  */
trait CromwellClientJVMSpecific extends RosHttp {
  self: CromwellClientShared =>

  import  implicits._

  def zipDependencies(files: Seq[(String, String)]): Option[File] = if(files.nonEmpty) {
    val dir = File.newTemporaryDirectory()
    for{(name, content) <- files}
    {
      val file = dir.createChild(name, asDirectory = false)
      file.write(content)
    }
    for(ch <- dir.children) println(s"written dependency as temp file ${ch.pathAsString}")
    Some(dir.zip())
  } else None

  def postWorkflowStrings(workflow: String,
                        workflowInputs: String,
                        workflowOptions: String = "",
                        wdlDependencies: Seq[(String, String)] = Seq.empty): Future[StatusInfo] ={
    self.postWorkflow(
      workflow,
      workflowInputs,
      workflowOptions,
      zipDependencies(wdlDependencies).map(f=>ByteBuffer.wrap(f.loadBytes))
    )
  }

  def validateWorkflow(workflow: String,
                          workflowInputs: String,
                          workflowOptions: String = "",
                          wdlDependencies: Seq[(String, String)] = Seq.empty): Future[ValidationResult] ={
    self.describeWorkflow(
      workflow,
      workflowInputs,
      workflowOptions,
      zipDependencies(wdlDependencies).map(f=>ByteBuffer.wrap(f.loadBytes))
    )
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

  def postWorkflowFolder(workflow: File,
                         workflowInputs: File,
                         workflowOptions: String = "",
                         wdlDependencies: Option[File] = None,
                        ): Future[StatusInfo] ={
    self.postWorkflow(
      workflow.lines.mkString("\n"),
      workflowInputs,
      workflowOptions,
      wdlDependencies.map(f=>ByteBuffer.wrap(zipFolder(f).loadBytes))
    )
  }

}
