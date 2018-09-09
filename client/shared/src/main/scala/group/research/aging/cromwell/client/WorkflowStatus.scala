package group.research.aging.cromwell.client


import enumeratum._
import io.circe.generic.JsonCodec

@JsonCodec sealed trait WorkflowStatus extends EnumEntry

object WorkflowStatus extends Enum[WorkflowStatus] {

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
