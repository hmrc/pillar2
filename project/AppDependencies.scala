import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                 % "2.13.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"       % "8.2.0",
    "com.beachape"      %% "enumeratum-play-json"      % "1.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"               % "3.2.19",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.mockito"             % "mockito-core"            % "5.20.0",
    "org.scalatestplus"      %% "mockito-4-11"            % "3.2.18.0",
    "org.scalatestplus"      %% "scalacheck-1-18"         % "3.2.19.0",
    "io.github.wolfendale"   %% "scalacheck-gen-regexp"   % "1.1.0",
    "com.softwaremill.diffx" %% "diffx-core"              % "0.9.0",
    "com.softwaremill.diffx" %% "diffx-scalatest"         % "0.9.0"
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock-standalone"    % "3.0.1",
    "uk.gov.hmrc"           %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )

}
