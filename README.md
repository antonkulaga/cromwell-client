cromwell-client
=================

Scala/ScalaJS Client for the Cromwell workflow engine.
This is a work-in-progress, not all methods have been implemented and no tests yet.

Adding to dependencies
----------------------

add the following to you build.sbt

resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main")
libraryDependencies += "comp.bio.aging" %%% "cromwell-client" % "0.0.8"

Usage
-----

Here are examples of the usage.

Get the list of existing workflows:
```scala
import group.research.aging.cromwell.client._
import scala.concurrent.Future
val client = CromwellClient.localhost
val outputs: Future[Stats] = client.getStats
```

Run workflow and get its status:
```scala
import java.io.{File => JFile}
import better.files._
import group.research.aging.cromwell.client._
import scala.concurrent.Future

val client = CromwellClient.localhost

val workflow = File("/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl")
val inputs = File("/home/antonkulaga/denigma/rna-seq/inputs/worms.json")
val result: Future[Status] = client.postWorkflowFiles(workflow, inputs)
```

Get the outputs by the id:
```scala
import group.research.aging.cromwell.client._
val client = CromwellClient.localhost
val id = "548a191d-deaf-4ad8-9c9c-9083b6ecbff8"
val outputs = client.getOutputs(id)
```

Cromwell-Web
=============

Cromwell-web subproject is a simple UI for accessing cromwell REST API.
_Note_: most of the calls are done via AJAX, so configure allow-origin header for cromwell.