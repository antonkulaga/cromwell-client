package comp.bio.aging.cromwell.client

object Runner extends scala.App{
  import java.io.{File => JFile}
  import scala.concurrent.duration._
  import better.files._
  import comp.bio.aging.cromwell.client.{CromwellClient, Status}
  import fr.hmil.roshttp.body.JSONBody._
  //val client = CromwellClient.localhost
  val client = new CromwellClient("http://localhost:38000/api", "v1")

  import fr.hmil.roshttp.body.Implicits._

  val stats = client.waitFor(client.getStats)

  val sourcePath = "/home/antonkulaga/denigma/rna-seq"
  val workflow = s"${sourcePath}/worms.wdl"
  val inputs = s"${sourcePath}/input.json"

  //docker run -v ~/data/nematodes/SRP058747/SRR2040663:/root itsjeffreyy/sratoolkit  /opt/sratoolkit/sam-dump SRR2040663.sra > SRR2040663.sam

  val file = File(workflow)
  val filePath = "/home/shelluser/nematodes"

  val input = File(inputs)

  def runWorkflow(): Status = {
    client.waitFor(client.postWorkflowFiles(file, input))
  }

  println("???????????????")
  val status = runWorkflow()
  pprint.pprintln(status)
  println("!!!!!!!!!!")
  println("OUTPUTS")
  val outputs = client.waitFor(client.getAllOutputs())
  pprint.pprintln(outputs)
  //java -jar cromwell.jar run /home/antonkulaga/denigma/rna-seq/worms_RNA_Seq.wdl  /home/antonkulaga/denigma/rna-seq/local.input.json

}
