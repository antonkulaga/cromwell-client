package group.research.aging.cromwell.web

object Main extends scala.App {
  // Starting the server
  System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
  System.setProperty("jdk.httpclient.allowRestrictedHeaders", "true")
  java.util.logging.LogManager.getLogManager.reset()

  WebServer.startServer("0.0.0.0", scala.util.Properties.envOrElse("CROMWELL_CLIENT_PORT", "8001").toInt)
}
