import group.research.aging.cromwell.client.{CromwellClient, Stats}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global

object GetRequestsTester extends scala.App {

  //val res = Tester.hello()//Tester.hello("http://localhost:8000", callback = "http://localhost:8001/api/trace")
  /*
  val res = Tester.hello()
  println("RESULTS:")
  println(res)
  println("======")
  */
  val client = new CromwellClient("http://pic:8000", "v1")
  val st = client.runtime.unsafeRunToFuture(client.getStatsZIO).future.recover { case ex =>
    println("ERROR " + ex)
    Stats(0,0)
  }
  val statusInfo = Await.result(st, 4 seconds)
  println(statusInfo)

  println("===========")
  val vs = client.runtime.unsafeRun(client.getVersionZIO)
  pprint.pprintln(vs)
  println("==============")
  val query = client.runtime.unsafeRun(client.getQueryZIO())
  pprint.pprintln(query)
  println("======AND NOW - METADATA!!!!====")

  pprint.pprintln(client.runtime.unsafeRun(client.getAllMetadataZIO()))
}
