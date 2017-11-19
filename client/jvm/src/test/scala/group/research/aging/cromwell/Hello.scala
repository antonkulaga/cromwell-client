package group.research.aging.cromwell

import cats.effect.IO
import group.research.aging.cromwell.client.{CromwellClient, Metadata}

import scala.concurrent.Future

object Hello extends App {
  val host: String = "agingkills.westeurope.cloudapp.azure.com"
  val port: Int = 8000
  lazy val url = s"http://${host}:${port}"
  val client = new CromwellClient(url)
  //client
  //val client = CromwellClient.localhost

//  pprint.pprintln(client.getStats.unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllOutputs().unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllLogs().unsafeRunSync())
  println("==================")
  pprint.pprintln(client.getAllMetadata().unsafeRunSync())
}
