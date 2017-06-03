package comp.bio.aging.cromwell.client

object Subber extends scala.App{
  import java.io.{File => JFile}
  import scala.concurrent.duration._
  import better.files._
  import comp.bio.aging.cromwell.client.{CromwellClient, Status}
  import fr.hmil.roshttp.body.JSONBody._
  val port = "38000"
  //val client = CromwellClient.localhost
  val client = new CromwellClient(s"http://localhost:${port}/api", "v1")

  import fr.hmil.roshttp.body.Implicits._

  val stats = client.waitFor(client.getStats)

  val base = "/home/antonkulaga/denigma/rna-seq/workflows"
  val sourcePath = s"${base}/sub"
  val workflow = s"${sourcePath}/main_workflow.wdl"
  val inputs = s"${sourcePath}/input_test.json"
  val subs = s"${sourcePath}/subs"

  val file = File(workflow)

  val input = File(inputs)

  def runWorkflow(): Status = {
    client.waitFor(client.postWorkflowFiles(file, input, File(subs)))
  }

  println("???????????????")

  val status = runWorkflow()
  //pprint.pprintln(status)
  println("!!!!!!!!!!")
  println("OUTPUTS")

  val outputs = client.waitFor(client.getAllOutputs())
  pprint.pprintln(outputs)
  //java -jar cromwell.jar run /home/antonkulaga/denigma/rna-seq/worms_RNA_Seq.wdl  /home/antonkulaga/denigma/rna-seq/local.input.json

}
