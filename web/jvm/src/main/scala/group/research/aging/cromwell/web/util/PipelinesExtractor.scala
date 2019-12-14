package group.research.aging.cromwell.web.util

import better.files.File
import group.research.aging.cromwell.web.{Pipeline, Pipelines}
import wvlet.log.LogSupport

/**
  * Extract pipelines from the default folder
  */
trait PipelinesExtractor {
  self: LogSupport =>

  /*
  def extractHost(hostFile: File = File("/etc/hosts")) = {
    val l = hostFile.lines.map{ case s =>
      val (h, v) = s.split("[ |\t]*")

    }
  }
  */

  protected def fileOption(file: File, str: String): Option[File] = Option(file / str).filter(_.exists)

  protected def fileOption(str: String): Option[File] = Option(File(str)).filter(_.exists)

  lazy val pipelinesRootDefaults: Option[File] =
    fileOption("/data/pipelines").orElse(
      fileOption("./pipelines").orElse(
        fileOption("./workflows").orElse(
          System.getProperty("user.home") match {
            case null => None
            case v=> fileOption(v + "/pipelines").orElse(fileOption(v + "/workflows")).filter(_.isDirectory).orElse(None)
          }
        )
      )
    )


  lazy val pipelinesRoot: Option[File] = scala.util.Properties.envOrNone("PIPELINES").flatMap(s=>fileOption(s)).orElse(pipelinesRootDefaults).filter(_.isDirectory)

  def getPipelineFile(pipeline: String): Option[File]= pipelinesRoot.flatMap{
      root => fileOption(root, pipeline + ".wdl").orElse(fileOption(root, pipeline))
    }


  protected def getDefaults(dir: File, containsName: String = "default"): String = {
    dir.children
      .filter(f=>f.isRegularFile && f.name.contains(containsName) && f.extension.contains(".json"))
      .foldLeft(""){
        (acc, el) => acc + el.lines.mkString("\n")
      }.trim
  }

  lazy val allPipelines: Pipelines = {
    val p = pipelinesRoot.map{ root => root.children
      .filter(f=> f.extension.contains("wdl") || f.isDirectory)
      .toList.map(n=>extractPipeline(n.name))
      .collect{ case Some(v) => v}}
      .getOrElse(Nil)
    for(v <- p) {
      println("FOUND PIPELINE:")
      println(v.name)
      if(v.defaults!=""){
        println("with default INTPUT values: ")
        println(v.defaults)
      }
      println("--------------------------------")
    }
    Pipelines(p)
  }

  /**
    * Extracts pipeline by name from the default Pipelines directory
    * @param pipeline
    * @return
    */
  def extractPipeline(pipeline: String): Option[Pipeline] = getPipelineFile(pipeline).flatMap{ v => v match {
      case dir if dir.isDirectory && dir.nonEmpty && dir.children.exists(_.extension.contains(".wdl")) =>
        val workflows: Seq[File] = dir.children.filter(f=>f.isRegularFile && f.extension.contains(".wdl")).toList
        val main: File = workflows.collectFirst{ case ch if ch.name == pipeline + ".wdl" | ch.name == "main.wdl" || ch.name == "index.wdl" => ch}.getOrElse {
          error("could not find good candidate for the main workflows, choosing the random wdl file")
          workflows.head
        }
        val inputDefaults: String = getDefaults(dir, "default")

        Some(Pipeline(
          main.name,
          main.lines.mkString("\n"),
          workflows.filter(_ != main).map(f=>f.name->f.lines.mkString("\n")).toList,
          inputDefaults
        ))

      case file if file.exists && file.isRegularFile =>
        val defs = getDefaults(file.parent, file.nameWithoutExtension + "_defaults")
        //(Some(file.lines.mkString("\n")), Nil, defs)
        Some(Pipeline(file.name, file.lines.mkString("\n"), Nil, defs))

      case d if d.name.startsWith(".") =>
        None

      case other =>
        warn(s"no pipeline found for ${other}!")
        None
    }
  }
}
