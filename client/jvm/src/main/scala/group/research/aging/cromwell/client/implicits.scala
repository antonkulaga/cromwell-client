package group.research.aging.cromwell.client

import better.files.File

object implicits {

  implicit def file2string(file: File): String = {
    if(file.exists) file.lines.mkString("\n") else ""
  }

  implicit def option2string(option: Option[String]): String = option.getOrElse("")

  implicit def fileOption2string(option: Option[File]): String = option.map(file2string).getOrElse("")

}
