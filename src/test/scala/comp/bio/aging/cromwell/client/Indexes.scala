package comp.bio.aging.cromwell.client

object Indexes extends scala.App{
  import better.files._
  //val host = "10.8.0.1" //localhost
  val host  = "//localhost"
  val port = "8000" //"38000"

  val client = new CromwellClient(s"http://${host}:${port}/api", "v1")

  val stats = client.waitFor(client.getStats)
  val base = "/home/antonkulaga/rna-seq/workflows"
  val sourcePath = s"${base}/rna-seq"
  val workflow = s"${sourcePath}/main_workflow.wdl"
  val inputs = s"${sourcePath}/inputs/worm.input.json"
  val subs = s"${sourcePath}/subs"

  val file = File(workflow)

  val input = File(inputs)

  def runWorkflow(): Status = {
    client.waitFor(client.postWorkflowFiles(file, input, File(subs)))
  }

  val status = runWorkflow()
  pprint.pprintln(status)
  println("OUTPUTS")
  val outputs = client.waitFor(client.getAllOutputs())
  pprint.pprintln(outputs)
}
