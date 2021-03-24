
object TestApp extends scala.App {

  //val res = Tester.hello()//Tester.hello("http://localhost:8000", callback = "http://localhost:8001/api/trace")
  val res = Tester.hello()
  println("RESULTS:")
  println(res)
  println("======")
}
