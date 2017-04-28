cromwell-client
=================

Scala/ScalaJS Client for the Cromwell workflow engine.
This is a work-in-progress, not all methods have been implemented and no tests yet.

Adding to dependencies
----------------------

add the following to you build.sbt

resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main")
libraryDependencies += "comp.bio.aging" %%% "cromwell-client" % "0.0.2"

Usage
-----

Here are examples of the usage.

Get the list of existing workflows:
```scala
import scala.concurrent.Future
import comp.bio.aging.cromwell.client._
val client = CromwellClient.localhost
val outputs: Future[Stats] = client.getStats
```

Run workflow and get its status:
```scala
import java.io.{File => JFile}
import better.files._
import comp.bio.aging.cromwell.client._
import scala.concurrent.Future

val client = CromwellClient.localhost
val workflow = "/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl"
val file = File(workflow)
val result: Future[Status] = client.postWorkflowFiles(file)
```

Get the outputs by the id:
```scala
import comp.bio.aging.cromwell.client._
val client = CromwellClient.localhost
val id = "548a191d-deaf-4ad8-9c9c-9083b6ecbff8"
val outputs = client.getOutputs(id)
```

Building from source
--------------------

The client is located at "client" subfolder.
To build from source you have to git clone the repository and compile it from sbt.