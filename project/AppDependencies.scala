import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.Keys.{libraryDependencies, scalaVersion}
import play.core.PlayVersion.current
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Def.setting {
    Seq(
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
      "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
      "uk.gov.hmrc"             %% "crypto-json-play-30"        % "8.0.0",
      "org.typelevel"           %% "cats-core"                  % "2.10.0",
    )
  }

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"              % "3.2.18",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.mockito"            % "mockito-core"            % "5.11.0",
    "org.scalatestplus"      %% "mockito-4-11"            % "3.2.18.0",
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "wolfendale"             %% "scalacheck-gen-regexp"  % "0.1.2",
    "com.github.tomakehurst" % "wiremock-standalone"     % "3.0.1",
    "com.softwaremill.diffx" %% "diffx-core"             % "0.9.0",
    "com.softwaremill.diffx" %% "diffx-scalatest"        % "0.9.0"
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % "it",
    "com.github.tomakehurst" % "wiremock-standalone" % "3.0.1" % "it"
  )
}
