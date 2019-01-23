package group.research.aging.cromwell.web.api


import akka.http.scaladsl.server.Rejection


object PipelinesRejections {
  final case class FolderDoesNotExist(folder: String)
    extends Rejection

  final case class PipelineNotFound(path: String)
    extends Rejection

}