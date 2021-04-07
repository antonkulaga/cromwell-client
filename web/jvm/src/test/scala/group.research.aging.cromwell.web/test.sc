
//import group.research.aging.cromwell.web.Tester
import cats.effect.IO
import hammock._
import hammock.apache.ApacheInterpreter
import hammock.circe.implicits._

object Tester {
  /*
    curl -d "{"hello.name": "World"}" -H "Accept: application/json"  -X POST http://localhost:8001/api/run/hello-world.wdl?host=http://pic:8000
    {"id":"9e0a1de3-9a0f-4ef3-8b96-f15a1a54a1a5","status":"Submitted"}
    */

  // Using the Apache HTTP commons interpreter
  implicit val interpreter = ApacheInterpreter[IO]

  def hello(server: String = "http://pic:8000") = {
    Hammock.request(Method.POST,
      uri"http://localhost:8001/api/run/hello-world.wdl?server=${server}",
      Map.empty,
      Some("""{"myWorkflow.name": "World"}""")).exec[IO].unsafeRunSync()
  }

}

println("????")
val res = MockPostRequests.hello("http://localhost:8000")
println(res)
println("!!!!")