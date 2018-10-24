package group.research.aging.cromwell.web.server

object Main extends scala.App {

  // Starting the server
  WebServer.startServer("0.0.0.0", 8001)
}
