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
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.helpers.AuthStubs
import uk.gov.hmrc.pillar2.helpers.wiremock.WireMockServerHandler
import uk.gov.hmrc.pillar2.models.btn.BTNRequest
import uk.gov.hmrc.pillar2.models.errors.Pillar2ApiError

import java.net.URI
import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class BTNControllerIntegrationSpec extends AnyFunSuite with GuiceOneServerPerSuite with WireMockServerHandler with AuthStubs {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wiremockPort)
    .configure("microservice.services.below-threshold-notification.port" -> wiremockPort)
    .configure("metrics.enabled" -> false)
    .build()

  val successfulResponse =  Json.obj(
    "success" -> Json.obj(
      "processingDate"   -> "2024-03-14T09:26:17Z"
    )
  )

  lazy val url = URI.create( s"http://localhost:$port${routes.BTNController.submitBtn().url}").toURL

  test("Successful BTN submission") {
    stubAuthenticate()
    val pillar2Id = "pillar2Id"
    val btnPayload = Json.toJson(BTNRequest(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
    server.stubFor(
      post(urlEqualTo("/RESTAdapter/plr/below-threshold-notification"))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .withRequestBody(equalToJson(Json.toJson(btnPayload).toString()))
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
    val request = httpClient.post(url)
      .withBody(btnPayload)
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)
    result.status mustEqual 201
  }

  test("Missing header error") {
    stubAuthenticate()
    val btnPayload = Json.toJson(BTNRequest(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))

    val httpClient = app.injector.instanceOf[HttpClientV2]
    given headerCarrier: HeaderCarrier = HeaderCarrier(authorization = Option(Authorization("bearertoken")))
      .withExtraHeaders( "Content-Type" -> "application/json")
    val request = httpClient.post(url)
      .withBody(btnPayload)
    val result  = Await.result(request.execute[HttpResponse], 5.seconds)
    result.status mustEqual 400
    val error = result.json.as[Pillar2ApiError]
    error.code mustEqual "001"
    error.message mustEqual "Missing X-Pillar2-Id header"
  }
}
