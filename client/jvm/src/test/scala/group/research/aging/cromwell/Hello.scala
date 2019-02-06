package group.research.aging.cromwell

import cats.effect.IO
import group.research.aging.cromwell.client.CromwellClient
import hammock.apache.ApacheInterpreter

object Hello extends App {

  implicit val getInterpreter = ApacheInterpreter[IO]

  //val host: String = "agingkills.westeurope.cloudapp.azure.com"

  val host: String = "localhost"

  val port: Int = 8000
  lazy val url = s"http://${host}:${port}"

  lazy val client = new CromwellClient(url, "v1")
  //lazy val client: CromwellClient = new CromwellClient("http://localhost:80", "v1")

  val base = "/home/antonkulaga/cromwell-client/client/jvm/src/test/resources/test1"
  //client.postWorkflowFiles(File(base + "/hello.wdl"), File(base + "/input1.json"), Some(File(base + "/option1.json")))

  val q = client.getQuery().unsafeRunSync()

  pprint.pprintln(q.results)

  for(r <- q.results) {
    pprint.pprintln(client.getOutputs(r.id).unsafeRunSync())
    val url = client.base + client.api + s"/workflows/${client.version}/${r.id}/outputs"
    println(url)
    println(client.get(client.api + s"/workflows/${client.version}/${r.id}/outputs", Map.empty).exec[IO].unsafeRunSync())
  }
  //pprint.pprintln(client.getQuery().unsafeRunSync())
  //println("==================")


  //pprint.pprintln(client.getAllOutputs().unsafeRunSync())

  /*
  println("==================")
  pprint.pprintln(client.getAllMetadata().unsafeRunSync())
  pprint.pprintln(client.getAllLogs().unsafeRunSync())
  println("==================")
*/
  //pprint.pprintln(client.getAllLogs().unsafeRunSync())


  /*
  for {
    r <- client.getQuery().unsafeRunSync().results
  } {
    println(client.get(s"/workflows/${client.version}/${r.id}/logs", Map.empty).exec[IO].unsafeRunSync().entity)
    val l = client.getLogs(r.id).unsafeRunSync()
    println("=============")
  }
  */

//  pprint.pprintln(client.getAllMetadata().unsafeRunSync())
}
