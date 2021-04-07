package group.research.aging.cromwell.web.common

import akka.actor.{Actor, OneForOneStrategy, SupervisorStrategy}
import wvlet.log.LogSupport

import scala.concurrent.duration._

trait BasicActor extends Actor with LogSupport {

  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      ⇒ akka.actor.SupervisorStrategy.Resume

      case _: NullPointerException     ⇒ akka.actor.SupervisorStrategy.Resume

      case fs: java.nio.file.FileSystemException  =>
        error("file system problem: " + fs.getMessage + " resuming....")
        akka.actor.SupervisorStrategy.Resume

      case con: java.net.ConnectException  =>
        error("problem with the connection: "+ con.getMessage + " resuming....")
        akka.actor.SupervisorStrategy.Resume

      case e: Exception                ⇒
        error(s"OTHER EXEPTION (${e.toString}), RESTARTING WHOLE ${this.self.path} ACTOR!!!")
        akka.actor.SupervisorStrategy.Restart
    }

  override def preStart { debug(s"${this.self.path.name} actor started at ${java.time.LocalDateTime.now()}") }
  override def postStop { debug(s"${this.self.path.name} actor stopped at ${java.time.LocalDateTime.now()}") }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    error(s"${this.self.path.name} actor RESTARTED at ${java.time.LocalDateTime.now()}")
    error(s" MESSAGE: ${message.getOrElse("")}")
    error(s" REASON: ${reason.getMessage}")
    super.preRestart(reason, message)
  }
}
