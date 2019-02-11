package group.research.aging.cromwell.web

import hammock.apache.ApacheInterpreter
import cats.effect.IO
import io.circe.generic.auto._
import hammock._
import hammock.marshalling._
import hammock.apache.ApacheInterpreter
import hammock.circe.implicits._
import hammock.hi.Opts
import io.circe.Json
import io.circe._, io.circe.parser._
import hammock.circe._

object Tester {
  /*
    curl -d "{"hello.name": "World"}" -H "Accept: application/json"  -X POST http://localhost:8001/api/run/hello-world.wdl?host=http://pic:8000
    {"id":"9e0a1de3-9a0f-4ef3-8b96-f15a1a54a1a5","status":"Submitted"}
    */

    // Using the Apache HTTP commons interpreter
    implicit val interpreter = ApacheInterpreter[IO]

  def run(pipeline: String, content: String, server: String = "http://pic:8000", callback: String = "http://localhost:8001/api/trace"): HttpResponse = {
    val json: Json = parse(content).right.get
    //val cont = Json.fromString("""{"myWorkflow.name": "World"}""")
    Hammock.request(Method.POST,
      uri"http://localhost:8001/api/run/${pipeline}?server=${server}&callback=${callback}",
      Map("Content-Type"->ContentType.`application/json`.name),
      Some(json)).exec[IO].unsafeRunSync()
  }

    def hello(server: String = "http://pic:8000", callback: String = "http://localhost:8001/api/trace"): HttpResponse =
      run("hello-world", content = """{"myWorkflow.name": "World!"}""", server = server, callback = callback)

}
