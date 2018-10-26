package group.research.aging.cromwell.web

import cats.kernel.Monoid
import group.research.aging.cromwell.client.{CromwellClient, CromwellClientLike, Metadata, WorkflowStatus}

import scala.collection.immutable._


object State{

  lazy val empty = State(CromwellClient.localhost, Nil)

  implicit def monoid: cats.Monoid[State] = new Monoid[State] {
    override def empty: State = State.empty

    override def combine(x: State, y: State): State = y //ugly TODO: rewrite
  }
}

case class State (client: CromwellClient,
                  metadata: List[Metadata],
                  status: WorkflowStatus = WorkflowStatus.AnyStatus,
                  errors: List[Messages.ExplainedError] = Nil,
                  effects: List[()=>Unit] = Nil)
{
  def withEffect(e: ()=>Unit): State = copy(effects = effects :+ e)

  lazy val sortedMetadata: List[Metadata] = metadata.sortWith{ case (a, b) =>
    a.startDate > b.startDate || a.startDate =="" || a.startDate == b.startDate && a.startTime > b.startTime
  }
}