package group.research.aging.cromwell.web
import scalacss.DevDefaults._  // Always use dev settings

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
    overflowX.scroll,
  )

  "#cromwell" - (
    margin(20 px),
    )

  "#url" - (
    minWidth(280 px)
  )
}
