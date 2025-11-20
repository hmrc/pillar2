import org.typelevel.scalacoptions.ScalacOptions
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "pillar2"

ThisBuild / scalaVersion := "3.3.7"
ThisBuild / majorVersion := 0

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    ScoverageKeys.coverageExcludedFiles :=
      "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;.*stubs.*;.*models.*;" +
        "app.*;.*BuildInfo.*;.*Routes.*;.*repositories.*;.*package.*;.*controllers.test.*;.*services.test.*;.*metrics.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    PlayKeys.playDefaultPort := 10051,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    compilerSettings
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test", base / "test-common")).value,
    Test / unmanagedResourceDirectories := Seq(baseDirectory.value / "test-resources")
  )
  .settings(CodeCoverageSettings.settings*)

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    DefaultBuildSettings.itSettings(),
    compilerSettings
  )
  .settings(
    libraryDependencies ++= AppDependencies.it,
    compilerSettings
  )

inThisBuild(
  List(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

lazy val compilerSettings = Seq(
  scalacOptions ~= (_.distinct),
  tpolecatCiModeOptions += ScalacOptions.warnOption("conf:src=routes/.*:s"),
  tpolecatExcludeOptions ++= Set(
    ScalacOptions.warnNonUnitStatement,
    ScalacOptions.fatalWarnings
  )
)

addCommandAlias("prePrChecks", "; scalafmtCheckAll; scalafmtSbtCheck; scalafixAll --check")
addCommandAlias("checkCodeCoverage", "; clean; coverage; test; it/test; coverageReport")
addCommandAlias("lint", "; scalafmtAll; scalafmtSbt; scalafixAll")
addCommandAlias("prePush", "; reload; clean; compile; test; lint")
