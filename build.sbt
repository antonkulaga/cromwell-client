import sbt.Keys.{javaOptions, javacOptions, resolvers, scalacOptions, sourceGenerators}
import sbt._
// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

name := "cromwell-client-parent"

//settings for all the projects
lazy val commonSettings = Seq(

	organization := "group.research.aging",

	scalaVersion :=  "2.12.8",

	version := "0.1.2",

	unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value,

	updateOptions := updateOptions.value.withCachedResolution(true), //to speed up dependency resolution

	resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main"),

	resolvers += "Broad Artifactory Releases" at "https://artifactory.broadinstitute.org/artifactory/libs-release/",

	resolvers += "Broad Artifactory Snapshots" at "https://artifactory.broadinstitute.org/artifactory/libs-snapshot/",

	/*
	libraryDependencies += "com.lihaoyi" % "ammonite" % "1.0.5" % Test cross CrossVersion.full,

	sourceGenerators in Test += Def.task {
		val file = (sourceManaged in Test).value / "amm.scala"
		IO.write(file, """object amm extends App { ammonite.Main().run() }""")
		Seq(file)
	}.taskValue,
	*/

	addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),

	addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),

	bintrayRepository := "main",

	bintrayOrganization := Some("comp-bio-aging"),

	licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),

	isSnapshot := true,

	exportJars := true,

	scalacOptions ++= Seq( "-feature", "-language:_" ),

	javacOptions ++= Seq("-Xlint", "-J-Xss5M", "-encoding", "UTF-8")
)

commonSettings

lazy val hammockVersion = "0.9.0"

lazy val semanticUI = "2.4.1"

lazy val webcomponents = "1.0.1"

lazy val jquery = "3.3.1"

lazy val airframeLogVersion = "19.2.1"

lazy val  cromwellClient = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("client"))
  .settings(commonSettings: _*)
  .settings(
    fork in run := true,

    parallelExecution in Test := false,

    name := "cromwell-client",

		libraryDependencies ++= Seq(
			"fr.hmil" %%% "roshttp" % "2.2.3",
			"com.beachape" %%% "enumeratum" % "1.5.13",
			"com.lihaoyi" %%% "pprint" % "0.5.3",
			//"org.typelevel" %%% "cats-core"      % "1.3.1",
			//"org.typelevel" %%% "cats-effect"     % "1.0.0",
			"io.circe" %%% "circe-generic-extras" % "0.11.1",
			"com.pepegar" %%% "hammock-circe" % hammockVersion,
			"org.wvlet.airframe" %%% "airframe-log" % airframeLogVersion
    )
	)
	.disablePlugins(RevolverPlugin)
  .jvmSettings(
    libraryDependencies ++= Seq(
			"com.github.pathikrit" %% "better-files" % "3.7.0",
			"com.pepegar" %% "hammock-apache-http" % hammockVersion,
			"com.pepegar" %% "hammock-akka-http" % hammockVersion
    )
  )
  .jsSettings(
		jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
		libraryDependencies ++= Seq(
			"org.scala-js" %%% "scalajs-java-time" % "0.2.5",
			"org.querki" %%% "jquery-facade" % "1.2"
		)
	)

lazy val cromwellClientJVM = cromwellClient.jvm

lazy val cromwellClientJS = cromwellClient.js

lazy val akka = "2.5.21"
lazy val akkaHttp = "10.1.7"

lazy val cromwellWeb = crossProject(JSPlatform, JVMPlatform)
	.crossType(CrossType.Full)
	.in(file("web"))
	.settings(commonSettings: _*)
	.settings(

		parallelExecution in Test := false,

		name := "cromwell-web",

		libraryDependencies  ++= Seq(
			"com.github.japgolly.scalacss" % "core_2.12" % "0.5.5",
			"org.wvlet.airframe" %%% "airframe-log" % airframeLogVersion
		)
	)
	.jsSettings(
		libraryDependencies ++= Seq(
			"in.nvilla" %%% "monadic-html" % "0.4.0-RC1",
			"org.akka-js" %%% "akkajsactorstream" % "1.2.5.19"
		),
		jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
		scalaJSUseMainModuleInitializer := true
	)
	.jsConfigure(p=>p.enablePlugins(ScalaJSWeb).disablePlugins(RevolverPlugin))
	.jvmSettings(
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-stream" % akka,
			"com.typesafe.akka" %% "akka-http" % akkaHttp,
			"com.typesafe.akka" %% "akka-http-xml" % akkaHttp,
			"javax.ws.rs" % "javax.ws.rs-api" % "2.1.1", //for extra annotations
			"com.github.swagger-akka-http" %% "swagger-akka-http" % "2.0.1",
			"com.github.swagger-akka-http" %% "swagger-scala-module" % "2.0.3",
			"com.vmunier" %% "scalajs-scripts" % "1.1.2",
      "de.heikoseeberger" %% "akka-http-circe" % "1.25.2",
			"ch.megard" %% "akka-http-cors" % "0.3.4",
			"org.webjars" % "Semantic-UI" %  semanticUI,
			"org.webjars" % "jquery" % jquery,
			"org.webjars" % "webcomponentsjs" % webcomponents,
			"org.webjars" % "swagger-ui" % "3.20.9" //Swagger UI
		),
		(managedClasspath in Runtime) += (packageBin in Assets).value,
		//pipelineStages in Assets := Seq(scalaJSProd),
		pipelineStages in Assets := Seq(scalaJSDev), //to make compilation faster
		//compile in Compile := ((compile in Compile) dependsOn scalaJSProd).value,
		(emitSourceMaps in fullOptJS) := true,
		fork in run := true,
		maintainer in Docker := "Anton Kulaga <antonkulaga@gmail.com>",
		dockerExposedPorts := Seq(8080),
		dockerRepository := Some("quay.io/comp-bio-aging"),
	).jvmConfigure(p=>
		p.enablePlugins(SbtWeb, JavaAppPackaging, DockerPlugin)
	)
	.dependsOn(cromwellClient)

lazy val webJS = cromwellWeb.js
lazy val webJVM = cromwellWeb.jvm.settings(
	scalaJSProjects := Seq(webJS)
)

mainClass in Compile := (mainClass in webJVM in Compile).value

(fullClasspath in Runtime) += (packageBin in webJVM in Assets).value

dockerUpdateLatest := true

