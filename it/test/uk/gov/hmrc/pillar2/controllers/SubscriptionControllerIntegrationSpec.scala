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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.AuthStubs
import uk.gov.hmrc.pillar2.helpers.wiremock.WireMockServerHandler
import uk.gov.hmrc.pillar2.models.errors.Pillar2ApiError
import uk.gov.hmrc.pillar2.models.hods.subscription.common.AmendSubscriptionSuccessV2

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.duration.DurationInt

class SubscriptionControllerIntegrationSpec
    extends AnyFunSuite
    with GuiceOneServerPerSuite
    with WireMockServerHandler
    with Generators
    with AuthStubs
    with OptionValues {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wiremockPort)
    .configure("microservice.services.amend-subscription-v2.port" -> wiremockPort)
    .configure("metrics.enabled" -> false)
    .build()

  lazy val url = URI.create(s"http://localhost:$port${routes.SubscriptionController.amendSubscriptionV2(id).url}").toURL

  val id = "Int-c75cfd09-f945-41a8-90db-a93177a7663c"

  test("A downstream 422 is mapped to a 422 carrying the validation error code and text") {
    stubAuthenticate()
    server.stubFor(
      put(urlEqualTo("/pillar2/subscription/v2"))
        .willReturn(
          aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(
              Json
                .obj("errors" -> Json.obj("processingDate" -> "2026-06-30T12:13:30Z", "code" -> "014", "text" -> "No subscription data found"))
                .toString()
            )
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient.put(url).withBody(Json.toJson(arbitrary[AmendSubscriptionSuccessV2].sample.value))
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)

    result.status mustEqual 422
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "014"
    error.message mustEqual "No subscription data found"
  }

  test("The downstream validation code is propagated to the caller unchanged") {
    stubAuthenticate()
    server.stubFor(
      put(urlEqualTo("/pillar2/subscription/v2"))
        .willReturn(
          aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/json")
            .withBody(
              Json
                .obj("errors" -> Json.obj("processingDate" -> "2026-06-30T12:13:30Z", "code" -> "089", "text" -> "ID number missing or invalid"))
                .toString()
            )
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient.put(url).withBody(Json.toJson(arbitrary[AmendSubscriptionSuccessV2].sample.value))
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)

    result.status mustEqual 422
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "089"
    error.message mustEqual "ID number missing or invalid"
  }

  test("An unmapped downstream status is mapped to a 500 with the generic API error code") {
    stubAuthenticate()
    server.stubFor(
      put(urlEqualTo("/pillar2/subscription/v2"))
        .willReturn(
          aResponse()
            .withStatus(503)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.obj("code" -> "503", "text" -> "Service unavailable").toString())
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient.put(url).withBody(Json.toJson(arbitrary[AmendSubscriptionSuccessV2].sample.value))
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)

    result.status mustEqual 500
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "003"
  }

  test("An unauthorised request is rejected with 401 and the downstream service is never called") {
    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"MissingBearerToken\"")
        )
    )

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient.put(url).withBody(Json.toJson(arbitrary[AmendSubscriptionSuccessV2].sample.value))
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)

    result.status mustEqual 401
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "401"
    server.verify(0, putRequestedFor(urlEqualTo("/pillar2/subscription/v2")))
  }

  test("An invalid request body is rejected with 400 and the downstream service is never called") {
    stubAuthenticate()

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders("Content-Type" -> "application/json")
    val request = httpClient.put(url).withBody(Json.obj("unexpected" -> "field"))
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)

    result.status mustEqual 400
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "400"
    server.verify(0, putRequestedFor(urlEqualTo("/pillar2/subscription/v2")))
  }

}
