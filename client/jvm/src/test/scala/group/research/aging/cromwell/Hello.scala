package group.research.aging.cromwell

import better.files.File
import cats.effect.IO
import group.research.aging.cromwell.client.{CromwellClient, Metadata}

import scala.concurrent.Future

object Hello extends App {
  val host: String = "agingkills.westeurope.cloudapp.azure.com"
  val port: Int = 8000
  lazy val url = s"http://${host}:${port}"
  lazy val client = new CromwellClient(url, "v1")
  //lazy val client: CromwellClient = new CromwellClient("http://localhost:80", "v1")
  val base = "/home/antonkulaga/cromwell-client/client/jvm/src/test/resources/test1"
  //client.postWorkflowFiles(File(base + "/hello.wdl"), File(base + "/input1.json"), Some(File(base + "/option1.json")))


  pprint.pprintln(client.getQuery().unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllOutputs().unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllMetadata().unsafeRunSync())
  pprint.pprintln(client.getAllLogs().unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllMetadata().unsafeRunSync())

}
