import java.io.{File => JFile}

import better.files._
import comp.bio.aging.cromwell.client.{CromwellClient, Status}
import fr.hmil.roshttp.body.JSONBody
import fr.hmil.roshttp.body.JSONBody._
val client = CromwellClient.localhost
import fr.hmil.roshttp.body.Implicits._

val stats = client.waitFor(client.getStats)

val workflow = "/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl"
val file = File(workflow)

def runWorkflow(): Status = {

  val input =  JSONObject(
    "wf.hello.pattern"-> "^[a-z]+$",
    "wf.hello.in"-> "/home/antonkulaga/Documents/test.txt"
  )
  client.waitFor(client.postWorkflowFiles(file, Some(input)))
}

//runWorkflow()
client.waitFor(client.mapQuery()(r=>client.getOutputsRequest(r.id))).map(r=>r.body)
val outputs = client.waitFor(client.getAllOutputs())
pprint.pprintln(outputs)
outputs.head
//val logs = client.waitFor(client.getAllLogs())
//pprint.pprintln(logs)


//val id = "f9ed8341-4a16-4fd2-a5b6-71946d0e325c"
/*
val b = client.waitFor(client.getMetadataRequest(id)).body
val metadata = client.waitFor(client.getMetadata(id))
val failures = metadata.failures
pprint.pprintln(failures)
  */
//client.waitFor(client.getLogsRequest(id)).body
//client.waitFor(client.getLogs(id))