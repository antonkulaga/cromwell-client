package group.research.aging.cromwell.web
import java.time.{OffsetDateTime, ZoneOffset}

import cats.kernel.Monoid
import group.research.aging.cromwell.client.{CromwellClient, CromwellClientLike, Metadata, WorkflowStatus}
import group.research.aging.cromwell.web.Commands.BatchRun
import group.research.aging.cromwell.web.Results.QueryWorkflowResults
import io.circe.{Json, ParsingFailure}
import io.circe.generic.JsonCodec
import io.circe.parser.parse

import scala.collection.immutable._
import scala.util.{Failure, Success}

object WorkflowNode {

  protected def sort(a: WorkflowNode, b: WorkflowNode): Boolean = {
    a.data.start.isEmpty || b.data.start.nonEmpty && a.data.start.get.isAfter(b.data.start.get)
  }

  def fromMetadata(metadata: Map[String, Metadata]): List[WorkflowNode] = {
    val (rootMap, others) = metadata.partition{ case (key, value) => value.rootWorkflowId.isEmpty || value.rootWorkflowId.get == key}
    rootMap.values.map(root => tree(root, others.values.toList)).toList.sortWith(sort)
  }

  def tree(root: Metadata, meta: List[Metadata]): WorkflowNode = meta.partition(_.parentWorkflowId.contains(root.id)) match {
    case (Nil, _) => WorkflowNode(root, Nil)
    case (list, others) => WorkflowNode(root, list.map(l=>tree(l, others)).sortWith(sort))
  }

}
case class WorkflowNode(data: Metadata, children: List[WorkflowNode])
{
  lazy val sortedMetadata: List[Metadata] = data::children.flatMap(_.sortedMetadata)
}

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
                  pipelines: Pipelines = Pipelines.empty,
                  heartBeat: HeartBeat = HeartBeat(None)
                 )
{

  def withEffect(e: ()=>Unit): State = copy(effects = effects :+ e)

  def sortedMetadata: List[Metadata] =  WorkflowNode.fromMetadata(results.metadata).flatMap(_.sortedMetadata)

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

object Pipelines{
  lazy val empty = Pipelines(Nil)
}
@JsonCodec case class Pipelines(pipelines: List[Pipeline])

object Pipeline {
  lazy val empty = Pipeline("", "", Nil)

  implicit def monoid: cats.Monoid[Pipeline] = new Monoid[Pipeline] {
    override def empty = Pipeline.empty

    override def combine(x: Pipeline, y: Pipeline): Pipeline = y //ugly TODO: rewrite
  }
}

@JsonCodec case class Pipeline(name: String, main: String,  dependencies: List[(String,String)], defaults: String = ""){

  def concatJson(js: String): Either[ParsingFailure, Json] = {
    import io.circe.parser._
    import io.circe.syntax._
    parse(defaults).flatMap(d=>parse(js).map(j=>d.deepMerge(j)))
  }

  def to_run(input: String, options: String = ""): Commands.Run = concatJson(input).map{
    js => Commands.Run(main, js.spaces2, options, dependencies)
  }.toTry match {
    case Success(value) =>
      //println(s"""MERGED JSON = \n ${value}""")
      value
    case Failure(exception) =>
      println("INPUT JSONG MERGING ERROR:")
      println(exception.toString)
      Commands.Run(main, input, options, dependencies)
  }

  def to_run_batch(inputs: Seq[String], servers:  scala.collection.Seq[String], title: String): BatchRun ={ //TODO: fix this ugly function
      val ins: Seq[String] = inputs.map{ i=>concatJson(i).toTry match {
        case Success(value) => value.spaces2
        case Failure(exception) =>
          println("INPUT JSONG MERGING ERROR:")
          println(exception.toString)
          i
      }
    }
    BatchRun(main, ins,  servers = servers, title = title, dependencies = dependencies)
  }
}