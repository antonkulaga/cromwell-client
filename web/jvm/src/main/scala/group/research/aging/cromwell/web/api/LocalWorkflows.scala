package group.research.aging.cromwell.web.api

import better.files.File
import wvlet.log.LogSupport

trait LocalWorkflows {
  self: BasicPipelineService =>

  def getPipelineFile(pipeline: String): File = {
    if(pipelinesRoot.exists) {  if( (pipelinesRoot / (pipeline + ".wdl")).exists) pipelinesRoot / (pipeline + ".wdl") else pipelinesRoot / pipeline } else if(File(pipeline).exists) File(pipeline) else File(pipeline + ".wdl")

  }

  protected def getDefaults(dir: File, containsName: String = "default"): String = {
    dir.children
      .filter(f=>f.isRegularFile && f.name.contains(containsName) && f.extension.contains(".json"))
      .foldLeft(""){
        (acc, el) => acc + el.lines.mkString("\n")
      }.trim
  }

  def extractPipeline(pipeline: String): (Option[String], List[(String,String)], String) = {
    getPipelineFile(pipeline) match {
      case dir if dir.isDirectory && dir.nonEmpty && dir.children.exists(_.extension.contains(".wdl")) =>
        val workflows: Seq[File] = dir.children.filter(f=>f.isRegularFile && f.extension.contains(".wdl")).toList
        val main: File = workflows.collectFirst{ case ch if ch.name == pipeline + ".wdl" | ch.name == "main.wdl" || ch.name == "index.wdl" => ch}.getOrElse {
          error("could not find good candidate for the main workflows, choosing the random wdl file")
          workflows.head
        }
        val inputDefaults: String = getDefaults(dir, "default")
        (Some(main.lines.mkString("\n")), workflows.filter(_ != main).map(f=>f.name->f.lines.mkString("\n")).toList, inputDefaults)

      case file if file.exists =>
        val defs = getDefaults(file.parent, file.nameWithoutExtension + "_defaults")
        (Some(file.lines.mkString("\n")), Nil, defs)

      case other =>
        error(s"no pipeline found for ${other}!")
        (None, Nil, "")
    }
  }


}