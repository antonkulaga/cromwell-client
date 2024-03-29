import sbt.Keys.{javaOptions, javacOptions, resolvers, scalacOptions, sourceGenerators}
import sbt._
import com.typesafe.sbt.packager.docker.{Cmd, DockerChmodType}
import sbtcrossproject.CrossType
import sbtcrossproject.CrossPlugin.autoImport.crossProject

name := "cromwell-client-parent"

Global / scalaJSStage := FullOptStage

//settings for all the projects
lazy val commonSettings = Seq(

	organization := "group.research.aging",

	scalaVersion :=  "2.13.13",

	version := "0.5.5",

	Compile / unmanagedClasspath ++= (Compile / unmanagedResources).value,

	updateOptions := updateOptions.value.withCachedResolution(true), //to speed up dependency resolution

	resolvers += "jitpack" at "https://jitpack.io",

	addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full),

	licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),

	isSnapshot := true,

	exportJars := true,

	scalacOptions ++= Seq("-feature", "-language:_"),

	scalacOptions += "-Ymacro-annotations",

	javacOptions ++= Seq("-Xlint", "-J-Xss256M", "-encoding", "UTF-8", "-XDignore.symbol.file"),

	javaOptions ++= Seq("-Djdk.internal.httpclient.debug=false", "-Djdk.httpclient.HttpClient.log=errors")
)

commonSettings

lazy val semanticUI = "2.4.1"

lazy val webcomponents = "1.0.1"

lazy val jquery = "3.6.3"

lazy val airframeLogVersion = "23.2.5"

lazy val sttpVersion = "3.5.2"

lazy val circeVersion = "0.14.3"

lazy val  cromwellClient = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("client"))
  .settings(commonSettings: _*)
  .settings(
    run / fork := true,

    Test / parallelExecution := false,

    name := "cromwell-client",

		libraryDependencies ++= Seq(
			"com.beachape" %%% "enumeratum" % "1.7.3",
			"com.lihaoyi" %%% "pprint" % "0.8.1",
			"io.circe" %%% "circe-generic-extras" % circeVersion,
			"io.circe" %%% "circe-parser" %  circeVersion,
			"io.circe" %%% "circe-generic" %  circeVersion,
			"org.wvlet.airframe" %%% "airframe-log" % airframeLogVersion
    )
	)
	.disablePlugins(RevolverPlugin)
  .jvmSettings(
    libraryDependencies ++= Seq(
			"com.github.pathikrit" %% "better-files" % "3.9.1",
			"com.softwaremill.sttp.client3" %% "core" % sttpVersion,
			"com.softwaremill.sttp.client3" %% "circe" % sttpVersion,
			"com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % sttpVersion,
			"com.typesafe.akka" %% "akka-stream" % akka
		)
  )
  .jsSettings(
		//jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
		libraryDependencies ++= Seq(
			"io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
		)
	)

lazy val cromwellClientJVM = cromwellClient.jvm

lazy val cromwellClientJS = cromwellClient.js

lazy val akka = "2.7.0"
lazy val akkaHttp = "10.4.0"

lazy val cromwellWeb = crossProject(JSPlatform, JVMPlatform)
	.crossType(CrossType.Full)
	.in(file("web"))
	.settings(commonSettings: _*)
	.settings(
		Test / parallelExecution := false,
		name := "cromwell-web",
		libraryDependencies  ++= Seq(
			"com.github.japgolly.scalacss" %%% "core" % "1.0.0",
			"org.wvlet.airframe" %%% "airframe-log" % airframeLogVersion
			//"org.scala-lang.modules" %% "scala-collection-compat" % "2.3.2"
		)
	)
	.jsSettings(
		libraryDependencies ++= Seq(
			"in.nvilla" %%% "monadic-html" % "0.5.0-RC1",
			"org.akka-js" %%% "akkajsactorstream" % "2.2.6.14"
		),
		//jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
		scalaJSUseMainModuleInitializer := true
	)
	.jsConfigure(p=>p.enablePlugins(ScalaJSWeb).disablePlugins(RevolverPlugin))
	.jvmSettings(
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-stream" % akka,
			"com.typesafe.akka" %% "akka-http" % akkaHttp,
			"com.typesafe.akka" %% "akka-http-xml" % akkaHttp,
			"javax.ws.rs" % "javax.ws.rs-api" % "2.1.1", //for extra annotations
			"com.github.swagger-akka-http" %% "swagger-akka-http" % "2.10.0",
			"com.github.swagger-akka-http" %% "swagger-scala-module" % "2.9.0",
			"com.vmunier" %% "scalajs-scripts" % "1.2.0",
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2", //"1.31.0",
			"ch.megard" %% "akka-http-cors" % "1.1.3",
			"org.webjars" % "Semantic-UI" %  semanticUI,
			"org.webjars.bowergithub.fomantic" % "fomantic-ui" % "2.9.2",
			"org.webjars" % "jquery" % jquery,
			"org.webjars" % "webcomponentsjs" % webcomponents,
			"org.webjars" % "swagger-ui" % "4.15.5", //Swagger UI
			"com.lihaoyi" %% "requests" % "0.8.0" % Test,
			"com.lihaoyi" %% "ammonite-ops" % "2.4.1" % Test),
		(Runtime / managedClasspath) += (Assets / packageBin).value,
		//(Runtime / fullClasspath) += (packageBin in Assets).value,
		Compile/ compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
		Assets / pipelineStages := Seq(scalaJSPipeline),
		scalaJSStage := FullOptStage,
		run / fork := true,
		//DOCKER PART
		Docker / maintainer := "Anton Kulaga <antonkulaga@gmail.com>",
		dockerBaseImage := "ghcr.io/graalvm/graalvm-ce:latest",
		Docker / daemonUserUid  := None,
		Docker / daemonUser := "root",
		dockerExposedVolumes := Seq("/data"),
		dockerUpdateLatest := true,
		dockerChmodType := DockerChmodType.UserGroupWriteExecute,
		dockerRepository := Some("quay.io/comp-bio-aging"),
		dockerCommands ++= Seq(
			Cmd("WORKDIR", "/data")
		),
		dockerExposedPorts := Seq(8001)
	).jvmConfigure(p=>
		p.enablePlugins(SbtWeb, JavaAppPackaging, DockerPlugin)
	)
	.dependsOn(cromwellClient)

lazy val webJS = cromwellWeb.js
lazy val webJVM = cromwellWeb.jvm.settings(
	scalaJSProjects := Seq(webJS)
)

Compile / mainClass := (mainClass in webJVM in Compile).value

(Runtime / managedClasspath) += (packageBin in webJVM in Assets).value

dockerUpdateLatest := true

