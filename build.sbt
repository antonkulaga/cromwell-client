import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys.{javaOptions, javacOptions, resolvers, scalacOptions}
import sbt._

name := "cromwell-client-parent"

//settings for all the projects
lazy val commonSettings = Seq(

	organization := "group.research.aging",

	scalaVersion :=  "2.12.4",

	version := "0.0.7",

	unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value,

	updateOptions := updateOptions.value.withCachedResolution(true), //to speed up dependency resolution

	resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main"),

	resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases"),

	resolvers += "Broad Artifactory Releases" at "https://artifactory.broadinstitute.org/artifactory/libs-release/",

	resolvers += "Broad Artifactory Snapshots" at "https://artifactory.broadinstitute.org/artifactory/libs-snapshot/",

	addCompilerPlugin(
		"org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
	),

	bintrayRepository := "main",

	bintrayOrganization := Some("comp-bio-aging"),

	licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),

	isSnapshot := true,

	exportJars := true,

	scalacOptions ++= Seq( "-target:jvm-1.8", "-feature", "-language:_" ),

	javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-J-Xss5M", "-encoding", "UTF-8")
)

commonSettings

lazy val circeVersion = "0.8.0"

lazy val hammockVersion = "0.7.0"

lazy val semanticUI = "2.2.10"

lazy val webcomponents = "1.0.1"

lazy val jquery = "3.1.1"

lazy val  cromwellClient = crossProject
  .crossType(CrossType.Full)
  .in(file("client"))
  .settings(commonSettings: _*)
  .settings(
    fork in run := true,

    parallelExecution in Test := false,
    
    name := "cromwell-client",

		libraryDependencies ++= Seq(
			"fr.hmil" %%% "roshttp" % "2.0.2",
			"com.beachape" %% "enumeratum" % "1.5.12",
			"com.lihaoyi" %%% "pprint" % "0.5.3",
			"com.pepegar" %%% "hammock-circe" % hammockVersion
    ),
		libraryDependencies ++= Seq(
			"io.circe" %%% "circe-core",
			"io.circe" %%% "circe-generic",
			"io.circe" %%% "circe-parser"
		).map(_ % circeVersion)
	)
	.disablePlugins(RevolverPlugin)
  .jvmSettings(
    libraryDependencies ++= Seq(
			"com.github.pathikrit" %% "better-files" % "2.17.1",
			"io.circe" %%% "circe-java8" % circeVersion,
			"org.webjars" % "Semantic-UI" %  semanticUI,
			"org.webjars" % "jquery" % jquery,
			"org.webjars" % "webcomponentsjs" % webcomponents
    )
  )
  .jsSettings(
		jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
  )

lazy val cromwellClientJVM = cromwellClient.jvm

lazy val cromwellClientJS = cromwellClient.js


lazy val cromwellWeb = crossProject
	.crossType(CrossType.Full)
	.in(file("web"))
	.settings(commonSettings: _*)
	.settings(

		parallelExecution in Test := false,

		name := "cromwell-web",

		libraryDependencies  ++= Seq(
			"com.github.japgolly.scalacss" % "core_2.12" % "0.5.3"
		)
	)
	.disablePlugins(RevolverPlugin)
	.jsSettings(
		libraryDependencies ++= Seq(
			"in.nvilla" %%% "monadic-html" % "0.3.2"
		),
		jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
		scalaJSUseMainModuleInitializer := true
	)
	.jsConfigure(p=>p.enablePlugins(ScalaJSWeb))
	.jvmSettings(
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-http" % "10.0.10",
			"com.vmunier" %% "scalajs-scripts" % "1.1.1",
			"com.pepegar" %% "hammock-akka-http" % hammockVersion
		),
		pipelineStages in Assets := Seq(scalaJSPipeline),
		compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
		(emitSourceMaps in fullOptJS) := true,
		fork in run := true
	)
	.jvmConfigure(p=>p.enablePlugins(SbtWeb, SbtTwirl))
	.dependsOn(cromwellClient)

lazy val webJS = cromwellWeb.js
lazy val webJVM = cromwellWeb.jvm.settings(
	scalaJSProjects := Seq(webJS)
)

dependsOn(webJVM)

mainClass in Compile := (mainClass in webJVM in Compile).value

(fullClasspath in Runtime) += (packageBin in webJVM in Assets).value

libraryDependencies += "com.lihaoyi" % "ammonite" % "1.0.3" % "test" cross CrossVersion.full

sourceGenerators in Test += Def.task {
	val file = (sourceManaged in Test).value / "amm.scala"
	IO.write(file, """object amm extends App { ammonite.Main().run() }""")
	Seq(file)
}.taskValue
