package group.research.aging.cromwell

package object client {
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

  case class WorkflowOutputs(values: Map[String, String]) extends CromwellResponse

  object WorkflowOutputs {
    import io.circe.syntax._
    implicit val encode: Encoder[WorkflowOutputs] = (a: WorkflowOutputs) => a.values.asJson

    implicit val decode: Decoder[WorkflowOutputs] = (c: HCursor) => c.focus match {
      case None => Left(DecodingFailure("Cannot extract workflow output!", c.history))
      case Some(json) => Right(WorkflowOutputs(json.asObject.map(o => o.toMap.mapValues(v => v.toString())).get))
    }

    lazy val empty = WorkflowOutputs(Map.empty[String, String])

  }


  case class Inputs(values: Map[String, String]) extends CromwellResponse

  object Inputs {
    import io.circe.syntax._
    implicit val encode: Encoder[Inputs] = (a: Inputs) => a.values.asJson

    implicit val decode: Decoder[Inputs] = (c: HCursor) => c.focus match {
      case None => Left(DecodingFailure("Cannot extract input!", c.history))
      case Some(json) => Right(Inputs(json.asObject.map(o => o.toMap.mapValues(v => v.toString())).get))
    }

    lazy val empty = Inputs(Map.empty[String, String])

  }


  object QueryResults {
    lazy val empty = QueryResults(Nil)
  }

  @ConfiguredJsonCodec  case class QueryResults(results: List[QueryResult]) extends CromwellResponse

  @ConfiguredJsonCodec  case class QueryResult(id: String, status: String, start: String = "", end: String = "") extends WorkflowResponse


  //implicit val config: Configuration = Configuration.default.withSnakeCaseKeys
  // config: io.circe.generic.extras.Configuration = Configuration(io.circe.generic.extras.Configuration$$$Lambda$2037/501381773@195cef0e,false,None)

  object Metadata

  @ConfiguredJsonCodec case class Metadata(
                                            id: String,
                                            submission: String = "",
                                            status: String = "",
                                            start: String = "",
                                            end: String = "",
                                            inputs: Inputs,
                                            outputs: WorkflowOutputs = WorkflowOutputs.empty,
                                            failures: List[WorkflowFailure] = Nil,
                                            submittedFiles: SubmittedFiles = SubmittedFiles.empty,
                                            workflowName: String = "",
                                            workflowRoot: String = "",
                                            calls: Map[String, List[LogCall]] = Map.empty[String, List[LogCall]]
                                          ) extends WorkflowResponse
  {

    lazy val startDate: String = start.substring(0, Math.max(0, start.indexOf("T")))
    lazy val endDate: String = end.substring(0, Math.max(0, end.indexOf("T")))

    lazy val startTime: String = start.substring(start.indexOf("T")+1, start.lastIndexOf("."))
    lazy val endTime: String = end.substring(end.indexOf("T")+1, end.lastIndexOf("."))

    lazy val dates: String = if(endDate==startDate || endDate=="") startDate else s"${startDate}-${endDate}"

    //protected def parse(text: String): LocalDate = LocalDate.parse(text, DateTimeFormatter.ISO_INSTANT)
  }

  //@ConfiguredJsonCodec case class WorkflowOutputs(outputs: Map[String, String]) extends CromwellResponse

  @ConfiguredJsonCodec case class CallOutputs(outputs: Map[String,  CallOutput], id: String) extends WorkflowResponse

  @ConfiguredJsonCodec case class StatusInfo(id: String, status: String) extends WorkflowResponse

  @ConfiguredJsonCodec case class Logs(id: String, calls: Option[Map[String, List[LogCall]]] = None) extends WorkflowResponse

  @ConfiguredJsonCodec case class LogCall(stderr: Option[String] , stdout: Option[String], attempt: Int, shardIndex: Int,
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
