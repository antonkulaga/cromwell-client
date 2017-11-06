package group.research.aging.cromwell.server

object Main extends scala.App {

  // Starting the server
  WebServer.startServer("0.0.0.0", 8080)
}
