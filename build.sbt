import play.sbt.PlayImport.PlayKeys.playDefaultPort
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import wartremover.{Wart, Warts}

lazy val appName = "pillar2"
// Scala version and major version settings
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    // Scoverage settings
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

    // Play framework settings
    playDefaultPort := 10051,

    // Scalafmt settings for automatic code formatting
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,

    // Dependencies for compiling, testing, and integration tests
    libraryDependencies ++= AppDependencies.compile.value ++ AppDependencies.test ++ AppDependencies.it,

    // WartRemover settings to catch common code issues
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

    // Suppressing warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",

    // Resource directory settings
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test", base / "test-common")).value,
    Test / unmanagedResourceDirectories := Seq(baseDirectory.value / "test-resources")
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings *)

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)