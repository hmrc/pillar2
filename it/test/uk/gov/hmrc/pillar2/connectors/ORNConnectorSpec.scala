/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.orn.{GetORNSuccess, GetORNSuccessResponse, ORNRequest}

import java.time.{LocalDate, ZonedDateTime}

class ORNConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure("microservice.services.overseas-return-notification.port" -> server.port())
    .build()

  private lazy val connector = app.injector.instanceOf[ORNConnector]

  val fromDate: LocalDate = LocalDate.now()
  val toDate:   LocalDate = LocalDate.now().plusYears(1)

  val url: String =
    s"/RESTAdapter/plr/overseas-return-notification/?accountingPeriodFrom=${fromDate.toString}&accountingPeriodTo=${toDate.toString}"

  private val ornPayload =
    ORNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1),
      filedDateGIR = LocalDate.now().plusYears(1),
      countryGIR = "US",
      reportingEntityName = "Newco PLC",
      TIN = "US12345678",
      issuingCountryTIN = "US"
    )

  val response: GetORNSuccessResponse = GetORNSuccessResponse(
    GetORNSuccess(
      processingDate = ZonedDateTime.parse("2024-03-14T09:26:17Z"),
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1),
      filedDateGIR = LocalDate.now().plusYears(1),
      countryGIR = "US",
      reportingEntityName = "Newco PLC",
      TIN = "US12345678",
      issuingCountryTIN = "US"
    )
  )

  private def stubResponseForSubmit(status: Int)(implicit response: JsObject): StubMapping =
    server.stubFor(
      post(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .withRequestBody(equalToJson(Json.toJson(ornPayload).toString()))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(response.toString())
        )
    )

  private def stubResponseForGet(status: Int): StubMapping =
    server.stubFor(
      get(urlEqualTo(url))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(Json.stringify(Json.toJson(response)))
        )
    )

  "submitOrn" - {
    "successfully submit a ORN request with X-PILLAR2-Id and receive Success response" in {
      implicit val response: JsObject = Json.obj(
        "success" -> Json.obj(
          "processingDate"   -> "2024-03-14T09:26:17Z",
          "formBundleNumber" -> "123456789012345"
        )
      )

      stubResponseForSubmit(CREATED)

      val result = connector.submitOrn(ornPayload).futureValue

      result.status mustBe CREATED
      result.json mustBe response
      server.verify(
        postRequestedFor(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withRequestBody(equalToJson(Json.toJson(ornPayload).toString()))
      )
    }

    "handle BAD_REQUEST (400) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "400",
          "message" -> "Bad Request",
          "logId"   -> "123456789"
        )
      )

      stubResponseForSubmit(BAD_REQUEST)

      val result = connector.submitOrn(ornPayload).futureValue
      result.status mustBe BAD_REQUEST
      result.json mustBe response
    }

    "handle UNPROCESSABLE_ENTITY (422) response" in {
      implicit val response: JsObject = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z",
          "code"           -> "003",
          "text"           -> "Request could not be processed"
        )
      )

      stubResponseForSubmit(UNPROCESSABLE_ENTITY)

      val result = connector.submitOrn(ornPayload).futureValue
      result.status mustBe UNPROCESSABLE_ENTITY
      result.json mustBe response
    }

    "handle INTERNAL_SERVER_ERROR (500) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal Server Error",
          "logId"   -> "123456789"
        )
      )

      stubResponseForSubmit(INTERNAL_SERVER_ERROR)

      val result = connector.submitOrn(ornPayload).futureValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe response
    }
  }

  "getOrn" - {
    "successfully get a ORN with X-PILLAR2-Id and receive Success response" in {

      stubResponseForGet(OK)

      val result = connector.getOrn(fromDate, toDate).futureValue

      result.status mustBe OK
      verifyHipHeaders("GET", url)
    }

    "must return status as 500 when ORN is not returned" in {

      stubResponseForGet(INTERNAL_SERVER_ERROR)

      val result = connector.getOrn(fromDate, toDate).failed

      result.failed.map { ex =>
        ex mustBe a[InternalServerException]
      }
    }
  }
}
