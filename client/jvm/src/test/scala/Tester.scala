import group.research.aging.cromwell.client.{CromwellClient, StatusInfo}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Tester {
  /*
    curl -d "{"hello.name": "World"}" -H "Accept: application/json"  -X POST http://localhost:8001/api/run/hello-world.wdl?host=http://pic:8000
    {"id":"9e0a1de3-9a0f-4ef3-8b96-f15a1a54a1a5","status":"Submitted"}
    */

  /*
  def run(pipeline: String, content: String, server: String = "http://agingkills.eu:8000", callback: String = "http://localhost:8001/api/trace"): HttpResponse = {
    val json: Json = parse(content).right.get
    //val cont = Json.fromString("""{"myWorkflow.name": "World"}""")
    Hammock.request(Method.POST,
      uri"http://localhost:8001/api/run/${pipeline}?server=${server}&callback=${callback}",
      Map("Content-Type"->ContentType.`application/json`.name),
      Some(json)).exec[IO].unsafeRunSync()
  }
   */

  lazy val greeting: String =
    """
      |version development
      |
      |workflow greeting {
      |    input {
      |        String name
      |    }
      |
      |    call greet {
      |        input:
      |            name = name
      |    }
      |
      |    output {
      |        String out = greet.out
      |    }
      |
      |}
      |
      |task greet {
      |    input {
      |        String name
      |    }
      |
      |    command {
      |        echo "Hello ~{name}!"
      |    }
      |
      |    output {
      |        String out = read_string(stdout())
      |    }
      |}
      |
      |""".stripMargin
      

    lazy val hello =
      """
        |version development
        |
        |import "greeting.wdl" as greeter
        |
        |workflow hello {
        |    input {
        |        String name
        |    }
        |
        |    call greeter.greet as greet {
        |        input:
        |            name = name
        |    }
        |
        |    output {
        |        String out = greet.out
        |    }
        |}
        |""".stripMargin

    def greeting(server: String = "http://agingkills.eu:8000", callback: String = "http://localhost:8001/api/trace"): StatusInfo = {
      val client = CromwellClient(server)

      val input =
        """
          |{
          |  "greeting.name": "World"
          |}
          |""".stripMargin
      val status: Future[StatusInfo] = client.postWorkflow(greeting, input, "", None)
      Await.result(status, 5 seconds)
    }

  def hello(server: String = "http://agingkills.eu:8000", callback: String = "http://localhost:8001/api/trace"): StatusInfo = {
    val client = CromwellClient(server, "v1")

    val input =
      """
        |{
        |  "hello.name": "World"
        |}
        |""".stripMargin
    val status: Future[StatusInfo] = client.postWorkflowStrings(hello, input, "", List("greeting.wdl"->greeting))
    Await.result(status, 5 seconds)
  }

  /*
  def quantify(server: String = "http://pic:8000", callback: String = "http://localhost:8001/api/trace") = {
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
   */

}
