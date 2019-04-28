package group.research.aging.cromwell

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import io.circe.{Encoder, Json}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder

import io.circe.{ Decoder, Encoder }

import io.circe.java8.time._

package object client {

  val dateTimeFormat = DateTimeFormatter.ISO_DATE_TIME

  implicit object DateTimeEncoder extends Encoder[OffsetDateTime] {
    import io.circe._
    import io.circe.syntax._

    override def apply(dt: OffsetDateTime): Json = dateTimeFormat.format(dt).asJson
  }

  implicit object UuidEncoder extends Encoder[UUID] {
    import io.circe._
    import io.circe.syntax._

    override def apply(u: UUID): Json = u.toString.asJson
  }

  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras._
  import io.circe.syntax._

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  import io.circe._
  import io.circe.generic.JsonCodec

  import scala.concurrent.duration._


  trait CromwellResponse

  @ConfiguredJsonCodec case class Stats(workflows: Int, jobs: Int) extends CromwellResponse

  @ConfiguredJsonCodec  case class Version(cromwell: String) extends CromwellResponse

  trait WorkflowResponse extends CromwellResponse
  {
    def id: String
  }

  case class CallOutput(value: Json) extends CromwellResponse

  object CallOutput {

    implicit val encode: Encoder[CallOutput] = new Encoder[CallOutput] {
      final def apply(a: CallOutput): Json = a.value
    }

    implicit val decode: Decoder[CallOutput] = new Decoder[CallOutput] {
      final def apply(c: HCursor): Decoder.Result[CallOutput] = c.focus match{
        case None => Left(DecodingFailure("Cannot extract call output!", c.history))
        case Some(json) => Right(CallOutput(json))
      }
    }

  }

  /*
  object Inputs {
    import io.circe.syntax._
    implicit val encode: Encoder[Inputs] = (a: Inputs) => a.values.asJson

    implicit val decode: Decoder[Inputs] = (c: HCursor) => c.focus match {
      case None => Left(DecodingFailure("Cannot extract input!", c.history))
      case Some(json) => Right(Inputs(json.asObject.map(o => o.toMap.mapValues(v => v.toString())).get))
    }

    lazy val empty = Inputs(Map.empty[String, String])

  }

  //@ConfiguredJsonCodec
  case class Inputs(values: Map[String, String] = Map.empty[String, String]) extends CromwellResponse
*/

  object QueryResults {
    lazy val empty = QueryResults(Nil)
  }

  @ConfiguredJsonCodec case class QueryResults(results: List[QueryResult]) extends CromwellResponse{
    lazy val ids: Set[String] = results.map(_.id).toSet
    lazy val rootWorkflows: List[QueryResult] = results.filter(w=>w.parentWorkflowId.isEmpty && w.rootWorkflowId.isEmpty)
  }

  @ConfiguredJsonCodec case class QueryResult(id: String, status: String, start: String = "", end: String = "", parentWorkflowId: Option[String] = None, rootWorkflowId: Option[String] = None) extends WorkflowResponse


  //implicit val config: Configuration = Configuration.default.withSnakeCaseKeys
  // config: io.circe.generic.extras.Configuration = Configuration(io.circe.generic.extras.Configuration$$$Lambda$2037/501381773@195cef0e,false,None)

  object Metadata

  @ConfiguredJsonCodec case class Metadata(
                                            id: String,
                                            workflowName: String = "",
                                            rootWorkflowId: Option[String] = None,
                                            calls: Map[String, List[LogCall]] = Map.empty[String, List[LogCall]],
                                            workflowRoot: String = "",
                                            //id is here
                                            inputs: Json = Json.obj(),//Inputs,
                                            status: String = "",
                                            parentWorkflowId: Option[String] = None,
                                            submission: Option[OffsetDateTime] = None,
                                            start: Option[OffsetDateTime] = None,
                                            end: Option[OffsetDateTime] = None,
                                            outputs: Json = Json.obj(),//WorkflowOutputs = WorkflowOutputs.empty,
                                            failures: List[WorkflowFailure] = Nil,
                                            submittedFiles: SubmittedFiles = SubmittedFiles.empty
                                          ) extends WorkflowResponse
  {

    lazy val startDate: String = start.map(_.toLocalDate.toString).getOrElse("")
    lazy val endDate: String = end.map(_.toLocalDate.toString).getOrElse("")

    lazy val startTime: String = start.map(_.toLocalDate.toString).getOrElse("")
    lazy val endTime: String = end.map(_.toLocalDate.toString).getOrElse("")

    lazy val dates: String = if(endDate==startDate || endDate=="") startDate else s"${startDate}-${endDate}"

    //protected def parse(text: String): LocalDate = LocalDate.parse(text, DateTimeFormatter.ISO_INSTANT)
  }

  //@ConfiguredJsonCodec case class WorkflowOutputs(outputs: Map[String, String]) extends CromwellResponse

  @ConfiguredJsonCodec case class CallOutputs(outputs: Map[String,  CallOutput], id: String) extends WorkflowResponse

  @ConfiguredJsonCodec case class StatusInfo(id: String, status: String) extends WorkflowResponse
  @ConfiguredJsonCodec case class ValidationResult(valid: Boolean, errors: List[String]) extends CromwellResponse

  @ConfiguredJsonCodec case class Logs(id: String, calls: Option[Map[String, List[LogCall]]] = None) extends WorkflowResponse

  @ConfiguredJsonCodec case class LogCall(stderr: String = "" , stdout: String = "", attempt: Int = 0, shardIndex: Int,
                                          callRoot: String = "",
                                          executionStatus: String = "",
                                          callCaching: Option[CallCaching] = None) extends CromwellResponse

  @ConfiguredJsonCodec case class Backends(supportedBackends: List[String], defaultBackend: String) extends CromwellResponse

  object SubmittedFiles {
    lazy val empty = SubmittedFiles("", "", "")
  }
  @ConfiguredJsonCodec case class SubmittedFiles(inputs: String, workflow: String, options: String) extends CromwellResponse

  @ConfiguredJsonCodec case class WorkflowFailure(message: String, causedBy: List[WorkflowFailure] = Nil) extends CromwellResponse

  object CallCaching {

    //implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
  }

  @ConfiguredJsonCodec case class CallCaching(allowResultReuse: Boolean, effectiveCallCachingMode: Option[String], hit: Option[Boolean] = None, result: String = "")



  import cats.Monoid
  import enumeratum._
  import io.circe.generic.JsonCodec

  @ConfiguredJsonCodec sealed trait WorkflowStatus extends EnumEntry

  object WorkflowStatus extends Enum[WorkflowStatus] {

    implicit def monoid: cats.Monoid[WorkflowStatus] = new Monoid[WorkflowStatus] {
      override def empty: WorkflowStatus = WorkflowStatus.AnyStatus

      override def combine(x: WorkflowStatus, y: WorkflowStatus): WorkflowStatus = y //ugly but works
    }

    /*
     `findValues` is a protected method that invokes a macro to find all `Greeting` object declarations inside an `Enum`
  
     You use it to implement the `val values` member
    */
    val values = findValues

    case object Submitted extends WorkflowStatus

    case object Running extends WorkflowStatus

    case object Aborting extends WorkflowStatus

    case object Failed extends WorkflowStatus

    case object Succeeded extends WorkflowStatus

    case object Aborted extends WorkflowStatus

    case object AnyStatus extends WorkflowStatus
  }

}
