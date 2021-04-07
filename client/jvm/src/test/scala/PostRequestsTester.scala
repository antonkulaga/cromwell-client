import group.research.aging.cromwell.client.{CromwellClient, Stats}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global

object PostRequestsTester extends scala.App {

  println("Testing hello")
  println(MockPostRequests.hello() )
  println("======")
  println("Testing bad input")
  println(MockPostRequests.bad_input())
  println("Bad input tested!")
  println("======")
  println("Testing bad wdl")
  println(MockPostRequests.bad_wdl())
  println("Bad wdl tested!")

}
