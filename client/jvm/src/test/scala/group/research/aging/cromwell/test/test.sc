import coursier.maven.MavenRepository
interp.repositories() ++= Seq(
  MavenRepository("https://dl.bintray.com/comp-bio-aging/main/"),
  MavenRepository("https://dl.bintray.com/hmil/maven/")
)
@
import $ivy.`group.research.aging::cromwell-client:0.0.27`
import pprint.PPrinter.BlackWhite
import group.research.aging.cromwell.client._
println("initializing the client")
val host: String = "http://pic:8000"
val client = CromwellClient(host)

//val m = client.getAllMetadata().unsafeRunSync()

//BlackWhite.pprintln(m, height = 1000)
//println("end")

val m = client.getQuery().unsafeRunSync()
BlackWhite.pprintln(m, height = 1000)
println("end")
