package group.research.aging.cromwell.web

import cats.effect.IO
import hammock._
import hammock.apache.ApacheInterpreter
import hammock.circe.implicits._
import io.circe.Json
import io.circe.parser._

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

  def quantify(server: String = "http://pic:8000", callback: String = "http://localhost:8001/api/trace"): HttpResponse = {
    run(pipeline = "quantification", content =
      """
        |{
        |  "quantification.key": "0a1d74f32382b8a154acacc3a024bdce3709",
        |  "quantification.samples_folder": "/data/samples",
        |  "quantification.salmon_indexes": {
        |    "Bos taurus": "/data/indexes/salmon/Bos_taurus",
        |    "Heterocephalus glaber": "/data/indexes/salmon/Heterocephalus_glaber",
        |    "Rattus norvegicus": "/data/indexes/salmon/Rattus_norvegicus",
        |    "Caenorhabditis elegans": "/data/indexes/salmon/Caenorhabditis_elegans",
        |    "Homo sapiens": "/data/indexes/salmon/Homo_sapiens",
        |    "Drosophila melanogaster": "/data/indexes/salmon/Drosophila_melanogaster",
        |    "Mus musculus": "/data/indexes/salmon/Mus_musculus"
        |  },
        |  "quantification.samples": [
        |    "GSM1698568",
        |    "GSM1698570",
        |    "GSM2927683",
        |    "GSM2927750",
        |    "GSM2042593",
        |    "GSM2042596"
        |  ]
        |}
      """.stripMargin, server = server, callback = callback)
  }

}
