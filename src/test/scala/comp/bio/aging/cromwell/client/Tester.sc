import java.io.{File => JFile}
import better.files._
import comp.bio.aging.cromwell.client.CromwellClient

val client = CromwellClient.localhost
val version = client.waitFor(client.getVersion)

val stats = client.waitFor(client.getStats)

val workflow = "/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl"
val file = File(workflow)

val id = "548a191d-deaf-4ad8-9c9c-9083b6ecbff8"
/*
val result = client.waitFor(client.postWorkflow(file.lines.mkString("\n")))
val id = result.id
val status = result.status
*/
