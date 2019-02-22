package group.research.aging.cromwell.web.misc

import better.files._
import group.research.aging.cromwell.client._

class BasicRunner(
                   val base: String,
                   val source: String,
                   val workflow: String,
                   val client: CromwellClient
                 ) extends scala.App {

  lazy val sourcePath: String = if (source.startsWith("/")) source else s"${base}/${source}"

  lazy val workflowFile = File(if (workflow.startsWith("/")) workflow else s"${sourcePath}/${workflow}")

  def getInputFile(input: String): File = File(if (input.startsWith("/")) input else s"${sourcePath}/inputs/${input}")

  def getSubs(subs: String): Option[File] = if (subs=="") None else {
    val f = File(if (subs.startsWith("/")) subs else s"${sourcePath}/${subs}")
    if(f.exists) Some(f) else None
  }

  def getOptions(option: String): Option[File] = if (option=="") None else {
    val f: File = File(if (option.startsWith("/")) option else s"${sourcePath}/inputs/${option}")
    if (f.exists) Some(f) else None
  }

  def run(input: String, options: String = "", subs: String = ""): Unit = {
    val status =
      client.postWorkflowFolder(
        workflowFile,
        getInputFile(input),
        getOptions(options).map(_.lines.mkString("\n")).getOrElse(""),
        getSubs(subs)
    )
    //println(status)
    //println("------")
    //pprint.pprintln(outputs)
  }

}
