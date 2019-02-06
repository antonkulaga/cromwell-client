package group.research.aging.cromwell.web

object TestApp extends scala.App {

  val res = Tester.hello("http://localhost:8000")
  println("RESULTS:")
  println(res)
  println("======")
}
