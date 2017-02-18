package comp.bio.aging.cromwell.client
import fr.hmil.roshttp.body.JSONBody

import scala.concurrent.Future

/**
  * Created by antonkulaga on 2/18/17.
  */
trait CromwellClientJVMSpecific {
  self: CromwellClientShared =>

  import java.io.{File => JFile}
  import better.files._

  def postWorkflowFiles(file: File, workflowInputs: Option[JSONBody] = None,
                   workflowOptions: Option[JSONBody] = None,
                   wdlDependencies: Option[JSONBody] = None): Future[Status] =
    self.postWorkflow(file.lines.mkString("\n"), workflowInputs, workflowOptions, wdlDependencies)
}
