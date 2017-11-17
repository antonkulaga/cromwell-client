package group.research.aging.cromwell

import group.research.aging.cromwell.client.{CromwellClient, Outputs}

import scala.concurrent.Future

object Tester extends App {


  //val host: String = "pipelines1.westeurope.cloudapp.azure.com"
  val host: String = "agingkills.westeurope.cloudapp.azure.com"
  val port: Int = 8000
  lazy val url = s"http://${host}:${port}"
  val client = new CromwellClient(url)
  val out: Future[List[Outputs]] = client.getAllOutputs()

  //pprint.pprintln(client.waitFor(client.getVersion))
  //pprint.pprintln(client.waitFor(client.getStats))

  val id = "22071c6d-52cc-4047-b43a-95e0f2e8566a"

  //pprint.pprintln(client.waitFor(client.getAllOutputs()))
  //pprint.pprintln(client.waitFor(client.getAllLogs()))

  pprint.pprintln(client.waitFor(client.getMetadataRequest(id)).body)
  println("===========================")
  //pprint.pprintln(client.waitFor(client.getMetadata(id)))
  pprint.pprintln(client.waitFor(client.getAllMetadata()))


  //pprint.pprintln(client.waitFor(client.getQuery()))
  //pprint.pprintln(client.waitFor(client.getAllMetadata()))


}
