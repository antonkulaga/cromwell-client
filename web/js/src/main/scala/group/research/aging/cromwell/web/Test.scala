package group.research.aging.cromwell.web

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding._
import org.scalajs.dom.html.Table
import org.scalajs.dom.raw.{Event, Node}

object Test {
  case class Contact(name: Var[String], email: Var[String])

  val data = Vars.empty[Contact]



  @dom
  def table: Binding[BindingSeq[Node]] = {
    <div>
      <button
      onclick={ event: Event =>
        data.value += Contact(Var("Yang Bo"), Var("yang.bo@rea-group.com"))
      }
      >
        Add a contact
      </button>
    </div>
      <table border="1" cellPadding="5">
        <thead>
          <tr>
            <th>Name</th>
            <th>E-mail</th>
            <th>Operation</th>
          </tr>
        </thead>
        <tbody>
          {
          for (contact <- data) yield {
            <tr>
              <td>
                {contact.name.bind}
              </td>
              <td>
                {contact.email.bind}
              </td>
              <td>
                <button
                onclick={ event: Event =>
                  contact.name.value = "Modified Name"
                }
                >
                  Modify the name
                </button>
              </td>
            </tr>
          }
          }
        </tbody>
      </table>
  }
  def init(node: Node) = {
    //val div = org.scalajs.dom.document.getElementById("test")
    //dom.render(node, table)
  }
}
