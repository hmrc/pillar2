/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.pillar2.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.helpers.AuthStubs
import uk.gov.hmrc.pillar2.helpers.wiremock.WireMockServerHandler
import uk.gov.hmrc.pillar2.models.errors.Pillar2ApiError
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus.{Fulfilled, Open}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType.{GlobeInformationReturn, Pillar2TaxReturn}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.SubmissionType.UKTR
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.{AccountingPeriodDetails, Obligation, ObligationsAndSubmissionsResponse, Submission}

import java.net.{URI, URL}
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt

class ObligationsAndSubmissionsControllerIntegrationSpec extends AnyFunSuite with GuiceOneServerPerSuite with WireMockServerHandler with AuthStubs {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wiremockPort)
    .configure("microservice.services.obligations-and-submissions.port" -> wiremockPort)
    .configure("metrics.enabled" -> false)
    .build()

  val response: ObligationsAndSubmissionsResponse = ObligationsAndSubmissionsResponse(
    LocalDateTime.now(),
    Seq(
      AccountingPeriodDetails(
        LocalDate.now(),
        LocalDate.now(),
        LocalDate.now(),
        underEnquiry = false,
        Seq(
          Obligation(
            Pillar2TaxReturn,
            Fulfilled,
            canAmend = false,
            Seq(
              Submission(
                UKTR,
                LocalDateTime.now()
              )
            )
          ),
          Obligation(
            GlobeInformationReturn,
            Open,
            canAmend = true,
            Seq.empty
          )
        )
      )
    )
  )

  val fromDate: LocalDate = LocalDate.now()
  val toDate: LocalDate = LocalDate.now()

  lazy val url: URL = URI.create( s"http://localhost:$port${routes.ObligationsAndSubmissionsController.getObligationsAndSubmissions(fromDate.toString, toDate.toString).url}").toURL

  test("Successful obligations and submissions request") {
    stubAuthenticate()
    val pillar2Id = "pillar2Id"
    server.stubFor(
      get(urlEqualTo(s"/RESTAdapter/plr/obligations-and-submissions/?fromDate=${fromDate.toString}&toDate=${toDate.toString}"))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.stringify(Json.toJson(response)))
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("X-Pillar2-Id" -> pillar2Id, "Content-Type" -> "application/json")
    val request = httpClient.get(url)
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)
    result.status mustEqual 200
  }

  test("Missing header error") {
    stubAuthenticate()

    val httpClient = app.injector.instanceOf[HttpClientV2]
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders( "Content-Type" -> "application/json")
    val request = httpClient.get(url)

    val result  = Await.result(request.execute[HttpResponse], 5.seconds)
    result.status mustEqual 400
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "001"
    error.message mustEqual "Missing X-Pillar2-Id header"
  }
}
