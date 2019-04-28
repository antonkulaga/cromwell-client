package group.research.aging.cromwell.web
import java.time.{OffsetDateTime, ZoneOffset}

import cats.kernel.Monoid
import group.research.aging.cromwell.client.{CromwellClient, CromwellClientLike, Metadata, WorkflowStatus}
import group.research.aging.cromwell.web.Results.QueryWorkflowResults

import scala.collection.immutable._


object State{

  lazy val empty = State(CromwellClient.localhost, QueryWorkflowResults.empty)

  implicit def monoid: cats.Monoid[State] = new Monoid[State] {
    override def empty: State = State.empty

    override def combine(x: State, y: State): State = y //ugly TODO: rewrite
  }
}


case class State (client: CromwellClient,
                  results: QueryWorkflowResults,
                  status: WorkflowStatus = WorkflowStatus.AnyStatus,
                  errors: List[Messages.ExplainedError] = Nil,
                  infos: List[Messages.Info] = Nil,
                  effects: List[()=>Unit] = Nil,
                  heartBeat: HeartBeat = HeartBeat(None)
                 )
{


  def withEffect(e: ()=>Unit): State = copy(effects = effects :+ e)

  lazy val sortedMetadata: List[Metadata] = results.metadata.values.toList.sortWith{ case (a, b) =>
    a.startDate > b.startDate || a.startDate =="" || a.startDate == b.startDate && a.startTime > b.startTime
  }
}

object HeartBeat
{
  def apply(last: Option[OffsetDateTime]): HeartBeat = {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    HeartBeat(last, now)
  }

  def apply(): HeartBeat = HeartBeat(Some(OffsetDateTime.now(ZoneOffset.UTC)))
}

case class HeartBeat(last: Option[OffsetDateTime], now: OffsetDateTime, maxDelay: Int = 12)
{
  def currentTime: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

  def delay: Option[Int] = last.map(l=> (now.getHour - l.getHour) * 3600 +  (now.getMinute - l.getMinute) * 60 + now.getSecond - l.getSecond)

  def warning: Option[Boolean] = delay.map(_ > maxDelay)

  def updatedLast = this.copy(last = Some(currentTime), currentTime)

  def updatedNow = this.copy(now = currentTime)
}