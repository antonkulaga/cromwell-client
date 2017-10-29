package group.research.aging.cromwell.runner

import java.io.{File => JFile}

import better.files._
import comp.bio.aging.cromwell.client._

class BasicRunner(
                   val base: String,
                   val source: String,
                   val workflow: String,
                   val host: String = "pipelines1.westeurope.cloudapp.azure.com",
                   val port: Int = 8000
                 )  extends scala.App {

  lazy val urlEngine = s"http://${host}:${port}"
  lazy val urlWorfklows = s"http://${host}:${port}/api"

  lazy val engine = new CromwellClient(urlEngine, "v1")
  lazy val client = new CromwellClient(urlWorfklows, "v1")

  def stats = client.waitFor(engine.getStats)

  lazy val sourcePath = if(source.startsWith("/")) source else s"${base}/${source}"
  lazy val workflowFile = File(if(workflow.startsWith("/")) workflow else s"${sourcePath}/${workflow}")

  protected def runWorkflow(file: File, input: File): Status = {
    client.waitFor(client.postWorkflowFiles(file, input))
  }

  protected def runWorkflow(file: File, input: File, subs: File): Status = {
    client.waitFor(client.postWorkflowFiles(file, input, subs))
  }

  def runWithSubs(input: String, subs: String = "subs") = {
    lazy val subWorkflows = File( if(subs.startsWith("/")) subs else s"${sourcePath}/${subs}")
    val inputFile = File( if(input.startsWith("/")) input else s"${sourcePath}/inputs/${input}")
    val status = runWorkflow(workflowFile, inputFile, subWorkflows)
    println("OUTPUTS:")
    val outputs = client.waitFor(client.getAllOutputs())
    pprint.pprintln(outputs)
  }


  def run(input: String) = {
    val inputFile = File( if(input.startsWith("/")) input else s"${sourcePath}/inputs/${input}")
    val status = runWorkflow(workflowFile, inputFile)
    println("OUTPUTS:")
    val outputs = client.waitFor(client.getAllOutputs())
    pprint.pprintln(outputs)
  }

}
