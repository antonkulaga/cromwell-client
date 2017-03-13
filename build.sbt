import com.typesafe.sbt.SbtNativePackager.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys.{javaOptions, javacOptions, resolvers, scalacOptions}
import sbt._

//settings for all the projects
lazy val commonSettings = Seq(

	organization := "comp.bio.aging",

	scalaVersion :=  "2.11.8",

	version := "0.0.1",

	unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value,

	updateOptions := updateOptions.value.withCachedResolution(true), //to speed up dependency resolution

	resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main"),

	resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases"),

	resolvers += "Broad Artifactory Releases" at "https://artifactory.broadinstitute.org/artifactory/libs-release/",

	resolvers += "Broad Artifactory Snapshots" at "https://artifactory.broadinstitute.org/artifactory/libs-snapshot/",

	maintainer := "Anton Kulaga <antonkulaga@gmail.com>",

	packageDescription := """cromwell-client""",

	bintrayRepository := "main",

	bintrayOrganization := Some("comp-bio-aging"),

	licenses += ("MPL-2.0", url("http://opensource.org/licenses/MPL-2.0")),

	isSnapshot := true,

	exportJars := true,

	addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

	scalacOptions ++= Seq( "-target:jvm-1.8", "-feature", "-language:_" ),

	javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-J-Xss5M", "-encoding", "UTF-8")
)

commonSettings

lazy val circeVersion = "0.7.0"

lazy val  cromwellClient = crossProject
  .crossType(CrossType.Full)
  .in(file("client"))
  .settings(commonSettings: _*)
  .settings(
    mainClass in Compile := Some("comp.bio.aging.cromwell"),

    fork in run := true,

    parallelExecution in Test := false,

    packageSummary := "cromwellClient",

    name := "cromwell-client",

		crossScalaVersions := Seq("2.12.1", "2.11.8"),

		libraryDependencies ++= Seq(
			"fr.hmil" %%% "roshttp" % "2.0.1",
			"com.beachape" %% "enumeratum" % "1.5.7",
			"com.lihaoyi" %%% "pprint" % "0.4.4"
    ),
		libraryDependencies ++= Seq(
			"io.circe" %%% "circe-core",
			"io.circe" %%% "circe-generic",
			"io.circe" %%% "circe-parser"
		).map(_ % circeVersion)
	)
  .jvmSettings(
    libraryDependencies ++= Seq(
			"com.github.pathikrit" %% "better-files" % "2.17.1",
			"io.circe" %%% "circe-java8" % circeVersion,
			"com.lihaoyi" % "ammonite" % "0.8.2" % Test cross CrossVersion.full
    ),
		initialCommands in (Test, console) := """ammonite.Main().run()"""
  )
  .jsSettings(
		libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.0",
    jsDependencies += RuntimeDOM % Test
  )

lazy val cromwellClientJVM = cromwellClient.jvm

lazy val cromwellClientJS = cromwellClient.js

lazy val wdl4sV = "0.10-e040cf8-SNAP"

libraryDependencies ++= Seq(
	"org.broadinstitute" %% "wdl4s" % wdl4sV,
	"com.github.alexarchambault" %% "case-app" % "1.2.0-M1"
)

dependsOn(cromwellClientJVM)
