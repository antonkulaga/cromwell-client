package group.research.aging.cromwell.web

import org.scalajs.dom

import scala.scalajs.js

object CromwellWeb extends scala.App {

    dom.window.alert("HELLO WORLD!")
    import mhtml._
    import scala.xml.Node
    import org.scalajs.dom

    val count: Var[Int] = Var(0)

    val doge: Node =
        <img style="width: 100px;" src="http://doge2048.com/meta/doge-600.png"/>

    val rxDoges: Rx[Seq[Node]] =
      count.map(i => Seq.fill(i)(doge))

    val component = // ← look, you can even use fancy names!
      <div>
        <button onclick={ () => count.update(_ + 1) }>Click Me!</button>
        { count.map(i => if (i <= 0) <div></div> else <h2>WOW!!!</h2>) }
        { count.map(i => if (i <= 2) <div></div> else <h2>MUCH REACTIVE!!!</h2>) }
        { count.map(i => if (i <= 5) <div></div> else <h2>SUCH BINDING!!!</h2>) }
        { rxDoges }
      </div>

    val div = dom.document.getElementById("cromwell")
    mount(div, component)

}
