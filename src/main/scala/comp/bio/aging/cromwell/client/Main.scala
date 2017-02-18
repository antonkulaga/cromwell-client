package comp.bio.aging.cromwell.client

import java.nio.file.Paths

import wdl4s.WdlNamespace

import scala.util.Try

/**
  * Created by antonkulaga on 2/18/17.
  */
class Main {
  def loadWdl(path: String): Try[WdlNamespace] = {
    WdlNamespace.loadUsingPath(Paths.get(path), None, None)
  }
}
