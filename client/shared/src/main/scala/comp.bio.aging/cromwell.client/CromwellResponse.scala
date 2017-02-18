package comp.bio.aging.cromwell.client
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody._
import fr.hmil.roshttp.body.MultiPartBody
import fr.hmil.roshttp.body.PlainTextBody
import io.circe.generic.JsonCodec

trait CromwellResponse

@JsonCodec case class Stats(workflows: Int, jobs: Int) extends CromwellResponse

@JsonCodec case class Version(cromwell: String) extends CromwellResponse

trait WorkflowResponse extends CromwellResponse
{
  def id: String
}

@JsonCodec case class Outputs(outputs: Map[String, String], id: String) extends WorkflowResponse

@JsonCodec case class QueryResult(id: String, name: String, start: String, end: String) extends WorkflowResponse

@JsonCodec case class Status(id: String, status: String) extends WorkflowResponse

@JsonCodec case class QueryResults(results: List[QueryResult]) extends CromwellResponse

@JsonCodec case class Logs(calls: Map[String, List[LogCall]], id: String) extends WorkflowResponse

@JsonCodec case class LogCall(stderr: String, stdout: String, attempt: Int, shardIndex: Int) extends CromwellResponse

@JsonCodec case class Backends(supportedBackends: List[String], defaultBackend: String) extends CromwellResponse