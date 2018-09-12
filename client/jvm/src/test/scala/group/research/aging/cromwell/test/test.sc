import coursier.maven.MavenRepository
interp.repositories() ++= Seq(MavenRepository("https://dl.bintray.com/comp-bio-aging/main/"))
@
import $ivy.`group.research.aging::cromwell-client:0.0.21`
@
import group.research.aging.cromwell.client._
println("initializing the client")
val host: String = "http://agingkills.westeurope.cloudapp.azure.com"
val client = CromwellClient(host)
val m = client.getAllMetadata().unsafeRunSync()
import pprint.PPrinter.BlackWhite
BlackWhite.pprintln(m, height = 1000)
