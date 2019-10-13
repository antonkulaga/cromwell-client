This repository contains two projects:
* _cromwell-client_ - a client library that interacts with Cromwell workflow engine from Java or browser
* _cromwell-web _ - a simple UI for running Cromwell workflows in the browser


Cromwell-Web
=============

Cromwell-web subproject is a simple UI for accessing cromwell REST API.
To run it open sbt console and go to CromwellWebJVM subproject and type
```sbtshell
reStart
```
It is also published as a Docker container. You can run it as:
```bash
docker run -p 8001:8001 quay.io/comp-bio-aging/cromwell-web:0.2.4
```

![Screenshot](/screenshot.png?raw=true "CromwellWeb screenshot")

Services
--------
You can start cromwell, mysql and cromwell-client altogether by starting a docker stack:
```bash
cd services
stack deploy -c docker-compose.yml cromwell
```
Make sure that you have docker swarm initialized ( https://docs.docker.com/engine/reference/commandline/swarm_init/ ) on your machine before deploying the stack
You can also use docker-compose instead of docker swarm.

cromwell-client
===============

Scala/ScalaJS Client for the Cromwell workflow engine.
This is a work-in-progress, not all methods have been implemented and no tests yet.

Adding to dependencies
----------------------

add the following to you build.sbt
```scala
resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main")
libraryDependencies += "group.research.aging" %%% "cromwell-client" % "0.2.6"
```
Usage
-----

Here are examples of the usage.

Get all metadataabout existing workflows:
```scala
import group.research.aging.cromwell.client._
val client = CromwellClient.localhost //or put with url of the cromwell server
val workflowsInfo: IO[List[Metadata]] = client.getAllMetadata()
//if you want to get as a Future
val workflowsInfoAsFuture: Future[List[Metadata]] = workflowsInfo.unsafeToFuture()
//if you want to get as a plain result (blocking)
val workflowsInfoBlocking: Seq[Metadata] = workflowsInfo.unsafeRunSync()

```
Note: in many methods IO monad is returned that can be easily turned into Future or plain (blocking) result.

Run workflow and get its status:
```scala
import java.io.{File => JFile}
import better.files._
import group.research.aging.cromwell.client._
import scala.concurrent.Future

val client = CromwellClient.localhost

val workflow = File("/home/antonkulaga/denigma/rna-seq/RNA_Seq.wdl")
val inputs = File("/home/antonkulaga/denigma/rna-seq/inputs/worms.json")
val result = client.postWorkflowFiles(workflow, inputs)
```

Get the outputs by the id:
```scala
import group.research.aging.cromwell.client._
val client = CromwellClient.localhost
val id = "548a191d-deaf-4ad8-9c9c-9083b6ecbff8"
val outputs = client.getOutputs(id)
```

CromwellWeb REST API
---------------------

Cromwell Web also allows has some REST API to run bioinformatic workflows. 
It is similar to cromwell and call cromwell server underneath 
/api/ shows swagger interface with docs.
```
http://localhost:8001/api/all
```
```aidl
http://localhost:8001/api/run/<workflows_name>/
```