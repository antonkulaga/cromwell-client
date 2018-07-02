import coursier.maven.MavenRepository
interp.repositories() ++= Seq(MavenRepository("https://dl.bintray.com/comp-bio-aging/main/"))
@
import $ivy.`group.research.aging::cromwell-client:0.0.13`
@
import group.research.aging.cromwell.client._
println("initializing the client")
val client = CromwellClient.localhost
val data = client.getAllMetadata().unsafeRunSync()
println(data)