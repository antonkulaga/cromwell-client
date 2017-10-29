package group.research.aging.cromwell.server

object Main extends scala.App {

  // Starting the server
  WebServer.startServer("localhost", 8080)
}
