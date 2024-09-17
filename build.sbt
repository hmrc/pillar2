import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.*
import sbt.Keys.{libraryDependencies, scalaVersion}
import scoverage.ScoverageKeys
import wartremover.{Wart, Warts}

lazy val appName = "pillar2"

// Scala version and major version settings
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0

// Common settings for the project
lazy val commonSettings = Seq(
  ScoverageKeys.coverageExcludedFiles := Seq(
    "<empty>",
    "com.kenshoo.play.metrics.*",
    ".*definition.*",
    "prod.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*stubs.*",
    ".*models.*",
    "app.*",
    ".*BuildInfo.*",
    ".*Routes.*",
    ".*repositories.*",
    ".*package.*",
    ".*controllers.test.*",
    ".*services.test.*",
    ".*metrics.*"
  ).mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 80,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true,

  playDefaultPort := 10051,

  Compile / scalafmtOnCompile := true,
  Test / scalafmtOnCompile := true,

  wartremoverErrors ++= Warts.allBut(
    Wart.Any,
    Wart.Nothing,
    Wart.Serializable,
    Wart.JavaSerializable,
    Wart.NonUnitStatements,
    Wart.DefaultArguments,
    Wart.ImplicitParameter,
    Wart.StringPlusAny,
    Wart.Overloading
  ),
  wartremoverExcluded ++= (Compile / routes).value,

  scalacOptions += "-Wconf:src=routes/.*:s",

  Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
  Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test", base / "test-common")).value,
  Test / unmanagedResourceDirectories := Seq(baseDirectory.value / "test-resources")
)

// Define integration test settings
lazy val itSettings = Defaults.itSettings ++ Seq(
  Test / parallelExecution := false,
  IntegrationTest / scalaSource := baseDirectory.value / "it"
)

// Main microservice project configuration
lazy val microservice = (project in file("."))
  .configs(IntegrationTest) // Enable the IntegrationTest configuration
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(commonSettings)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*) // Add integration test settings
  .settings(
    libraryDependencies ++= AppDependencies.compile.value ++ AppDependencies.test ++ AppDependencies.it,
    resolvers += Resolver.jcenterRepo,
    CodeCoverageSettings.settings
  )

// Integration test subproject
lazy val it = (project in file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .configs(IntegrationTest)
  .settings(itSettings)
  .settings(libraryDependencies ++= AppDependencies.it)