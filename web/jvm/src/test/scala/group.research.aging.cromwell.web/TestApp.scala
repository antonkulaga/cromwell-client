package group.research.aging.cromwell.web

object TestApp extends scala.App {

  val res = Tester.hello("http://localhost:8000", callback = "http://localhost:8001/api/trace")
  println("RESULTS:")
  println(res)
  println("======")
}
