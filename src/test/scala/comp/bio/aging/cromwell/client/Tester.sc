import java.io.{File => JFile}

import better.files._
import comp.bio.aging.cromwell.client.{CromwellClient, Status}
import fr.hmil.roshttp.body.JSONBody
import fr.hmil.roshttp.body.JSONBody._
val client = CromwellClient.localhost
import fr.hmil.roshttp.body.Implicits._

val stats = client.waitFor(client.getStats)

//val workflow = "/home/antonkulaga/denigma/rna-seq/worms_RNA_Seq.wdl"
//val workflow = "/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl"


val workflow = "/home/antonkulaga/denigma/rna-seq/bedtools.wdl"
val file = File(workflow)

val data = "/home/antonkulaga/data/bedtutorial/"

val first = data + "cpg.bed"
val second = data + "exons.bed"

val input =  JSONObject(
  "bedtest.file1"-> first,
  "bedtest.file2"-> second
)

def runWorkflow(): Status = {
  client.waitFor(client.postWorkflowFiles(file, Some(input)))
}

//runWorkflow()

val last = client.waitFor(client.getQuery()).results.last
pprint.pprintln(last)
println(last.start)
println(last.end)
println(last.duration.toSeconds)


//val metadata = client.waitFor(client.getMetadata(last.id))
//pprint.pprintln(metadata)


//client.waitFor(client.mapQuery()(r=>client.getOutputsRequest(r.id))).map(r=>r.body)
val outputs = client.waitFor(client.getAllOutputs())
pprint.pprintln(outputs)
//outputs.headjd
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


//GSE69263