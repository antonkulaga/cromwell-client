package group.research.aging.cromwell.client

import cats.effect.IO
//import hammock.InterpTrans
//import hammock.js.Interpreter

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by antonkulaga on 2/18/17.
  */
trait CromwellClientJSspecific //extends RosHttp
{
  //self: CromwellClientShared =>


  //implicit val cs = IO.contextShift(global)

  //implicit override protected def getInterpreter: InterpTrans[IO] = Interpreter.instance[IO]
  //implicit override protected def getInterpreter: InterpreterFixed[IO] = InterpreterFixed[IO]

}

