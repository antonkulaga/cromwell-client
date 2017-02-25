package comp.bio.aging.cromwell.client
import fr.hmil.roshttp.body.JSONBody
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
}
