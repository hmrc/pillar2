import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.12.0"
  private val hmrcMongoVersion = "1.5.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % hmrcMongoVersion
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"              % "3.0.9"   % "test, it",
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion            % Test,
    "org.mockito"            % "mockito-core"            % "3.7.7"   % "test, it",
    "com.typesafe.play"      %% "play-test"              % current   % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"   % "test, it",
    "com.vladsch.flexmark"   % "flexmark-all"            % "0.35.10" % "test, it",
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.7.0" % "test",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2" % Test,
    "wolfendale"              %% "scalacheck-gen-regexp"      % "0.1.2"       % Test,
    "com.github.tomakehurst" % "wiremock-standalone"     % "2.27.2"  % "it",
    "com.softwaremill.diffx" %% "diffx-core"             % "0.4.1"   % "test, it",
    "com.softwaremill.diffx" %% "diffx-scalatest"        % "0.4.1"   % "test, it"

  )
}
