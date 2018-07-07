package group.research.aging.cromwell.client

import cats.effect.IO
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

