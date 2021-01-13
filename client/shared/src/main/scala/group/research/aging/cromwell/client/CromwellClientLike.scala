package group.research.aging.cromwell.client

import java.net.URI

import cats.effect.IO
//import hammock.{Decoder, _}
//import hammock.marshalling._

import scala.concurrent.Future

trait CromwellClientLike {

  def base: String
  def version: String

  def api: String

  def baseHost: String = new URI(base).getHost


}
