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
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.AuthStubs
import uk.gov.hmrc.pillar2.helpers.wiremock.WireMockServerHandler
import uk.gov.hmrc.pillar2.models.errors.Pillar2ApiError
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.ReturnType.NIL_RETURN
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.{LiabilityNilReturn, UKTRSubmissionData, UKTRSubmissionNilReturn}

import java.net.URI
import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class UKTRControllerIntegrationSpec extends AnyFunSuite with GuiceOneServerPerSuite with WireMockServerHandler
  with Generators with ScalaCheckPropertyChecks with AuthStubs {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wiremockPort)
    .configure("microservice.services.submit-uk-tax-return.port" -> wiremockPort)
    .configure("metrics.enabled" -> false)
    .build()

  val successfulResponse = Json.obj(
    "success" -> Json.obj(
      "processingDate"        -> "2024-03-14T09:26:17Z",
      "formBundleNumber"      -> "abcdef123456",
      "chargeReference" -> "123456"
    )
  )

  lazy val url = URI.create(s"http://localhost:$port${routes.UKTaxReturnController.submitUKTaxReturn().url}").toURL

  test("Successful UKTR liability submission") {
    val pillar2Id = "pillar2Id"


    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("X-Pillar2-Id" -> pillar2Id, "Content-Type" -> "application/json")

    forAll(arbitrary[UKTRSubmissionData]) {payload =>
      stubAuthenticate()
      server.stubFor(
        post(urlEqualTo("/RESTAdapter/plr/uk-tax-return"))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withRequestBody(equalToJson(Json.toJson(payload).toString()))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(successfulResponse.toString())
          )
      )
      val request = httpClient
        .post(url)
        .withBody(Json.toJson(payload))
      val result = Await.result(request.execute[HttpResponse], 5.seconds)
      result.status mustEqual 201
      server.resetAll()
    }}

  test("Successful UKTR nil submission") {
    stubAuthenticate()
    val pillar2Id = "pillar2Id"
    val uktrPayload = UKTRSubmissionNilReturn(
      LocalDate.now,
      LocalDate.now(),
      obligationMTT = false,
      electionUKGAAP = false,
      LiabilityNilReturn(returnType = NIL_RETURN)
    )
    server.stubFor(
      post(urlEqualTo("/RESTAdapter/plr/uk-tax-return"))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .withRequestBody(equalToJson(Json.toJson(uktrPayload).toString()))
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody(successfulResponse.toString())
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("X-Pillar2-Id" -> pillar2Id, "Content-Type" -> "application/json")

      val request = httpClient
        .post(url)
        .withBody(Json.toJson(uktrPayload))
      val result = Await.result(request.execute[HttpResponse], 5.seconds)
      result.status mustEqual 201

  }

  test("Missing header error") {
    stubAuthenticate()
    val uktrPayload = UKTRSubmissionNilReturn(
      LocalDate.now,
      LocalDate.now(),
      obligationMTT = false,
      electionUKGAAP = false,
      LiabilityNilReturn(returnType = NIL_RETURN)
    )
    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient
      .post(url)
      .withBody(Json.toJson(uktrPayload))
    val result = Await.result(request.execute[HttpResponse], 5.seconds)
    result.status mustEqual 400
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "001"
    error.message mustEqual "Missing X-Pillar2-Id header"
  }
}
