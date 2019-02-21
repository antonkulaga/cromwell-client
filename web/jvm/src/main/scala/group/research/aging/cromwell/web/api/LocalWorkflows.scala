package group.research.aging.cromwell.web.api

import better.files.File
import wvlet.log.LogSupport

trait LocalWorkflows {
  self: BasicService =>

  def extractPipeline(root: File, pipeline: String): (Option[String], List[(String,String)]) = {
    val fl = if(root.exists) {  if( (root / (pipeline + ".wdl")).exists) root / (pipeline + ".wdl") else root / pipeline } else if(File(pipeline).exists) File(pipeline) else File(pipeline + ".wdl")
    fl match {
      case dir if dir.isDirectory && dir.nonEmpty && dir.children.exists(_.extension.contains(".wdl")) =>
        val workflows: Seq[File] = dir.children.filter(f=>f.isRegularFile && f.extension.contains(".wdl")).toList
        val main: File = workflows.collectFirst{ case ch if ch.name == pipeline + ".wdl" | ch.name == "main.wdl" || ch.name == "index.wdl" => ch}.getOrElse {
          error("could not find good candidate for the main workflows, choosing the random wdl file")
          workflows.head
        }
        (Some(main.lines.mkString("\n")), workflows.filter(_ != main).map(f=>f.name->f.lines.mkString("\n")).toList)

      case file if file.exists =>
        (Some(file.lines.mkString("\n")), Nil)

      case other =>
        error(s"no pipeline found for ${other}!")
        (None, Nil)
    }
  }


}