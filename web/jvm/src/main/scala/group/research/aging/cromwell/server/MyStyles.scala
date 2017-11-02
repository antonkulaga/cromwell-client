package group.research.aging.cromwell.server

import scalacss.DevDefaults._

object MyStyles extends StyleSheet.Standalone {


  import dsl._

  media.maxWidth(1024 px) - {
    &("html") - {
      fontSize(8 pt)
    }
  }
  media.minWidth(1281 px) - {
    &("html") - {
      fontSize(12 pt)
    }
  }

  "body" - (
//    backgroundColor(skyblue)
    )

  "#main" - (
    margin(20 px)
    //backgroundColor(bindingGreen)
    //backgroundColor(bindingGreen)
    )
}