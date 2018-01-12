package group.research.aging.cromwel.client

import cats.effect.IO
import group.research.aging.cromwell.client.CromwellClientShared
import hammock.js.Interpreter

/**
  * Created by antonkulaga on 2/18/17.
  */
trait CromwellClientJSspecific
{
  self: CromwellClientShared =>

  implicit override protected def getInterpreter: Interpreter[IO] = Interpreter[IO]
  //implicit override protected def getInterpreter: InterpreterFixed[IO] = InterpreterFixed[IO]

}

