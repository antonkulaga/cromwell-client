import java.io.{File => JFile}

import better.files._
import comp.bio.aging.cromwell.client.{CromwellClient, Status}
import fr.hmil.roshttp.body.JSONBody
import fr.hmil.roshttp.body.JSONBody._
val client = CromwellClient.localhost
//val client = new CromwellClient("http://localhost:38000/api", "v1")
val last = client.waitFor(client.getQuery()).results.last
pprint.pprintln(last)
println(last.start)
println(last.end)
println(last.duration.toSeconds)
val outputs = client.waitFor(client.getAllOutputs())
pprint.pprintln(outputs)