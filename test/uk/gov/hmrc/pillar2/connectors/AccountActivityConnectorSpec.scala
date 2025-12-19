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

package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.IntegrationPatience
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.accountactivity.AccountActivityRequest

import java.time.LocalDate

class AccountActivityConnectorSpec extends BaseSpec with IntegrationPatience {

  override lazy val app: Application = applicationBuilder()
    .configure("microservice.services.account-activity.port" -> server.port())
    .build()

  val sampleAccountActivityResponse: JsValue = Json.parse(getClass.getResourceAsStream("/data/sample-account-activity.json"))

  lazy val connector: AccountActivityConnector = app.injector.instanceOf[AccountActivityConnector]

  val fromDate: LocalDate = LocalDate.now().minusYears(1)
  val toDate:   LocalDate = LocalDate.now()

  val accountActivityUrl: String = s"/RESTAdapter/plr/account-activity?fromDate=$fromDate&toDate=$toDate"

  def stubFor(status: Int, responseBody: JsValue): StubMapping = server.stubFor(
    get(urlEqualTo(accountActivityUrl))
      .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
      .withHeader("X-Message-Type", equalTo("ACCOUNT_ACTIVITY"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(Json.stringify(responseBody))
      )
  )

  "AccountActivityConnectorSpec" - {
    "returns a success response" in {
      stubFor(OK, sampleAccountActivityResponse)

      val result = connector.retrieveAccountActivity(AccountActivityRequest(fromDate, toDate), pillar2Id).futureValue

      result.status mustBe OK
      result.json mustBe sampleAccountActivityResponse

      server.verify(
        getRequestedFor(urlEqualTo(accountActivityUrl))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withHeader("X-Message-Type", equalTo("ACCOUNT_ACTIVITY"))
      )

      verifyHipHeaders("GET", accountActivityUrl, expectedBody = None)
    }

    "returns a 400 response" in {
      val badRequestResponse = Json.obj(
        "error" -> Json.obj(
          "code"    -> "400",
          "message" -> "Bad Request",
          "logId"   -> "123456789"
        )
      )

      stubFor(BAD_REQUEST, badRequestResponse)

      val result = connector.retrieveAccountActivity(AccountActivityRequest(fromDate, toDate), pillar2Id).futureValue

      result.status mustBe BAD_REQUEST
      result.json mustBe badRequestResponse

      server.verify(
        getRequestedFor(urlEqualTo(accountActivityUrl))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withHeader("X-Message-Type", equalTo("ACCOUNT_ACTIVITY"))
      )

      verifyHipHeaders("GET", accountActivityUrl, expectedBody = None)
    }

    "returns a 422 response" in {
      val unprocessableResponse = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z",
          "code"           -> "003",
          "text"           -> "Request could not be processed"
        )
      )

      stubFor(UNPROCESSABLE_ENTITY, unprocessableResponse)

      val result = connector.retrieveAccountActivity(AccountActivityRequest(fromDate, toDate), pillar2Id).futureValue

      result.status mustBe UNPROCESSABLE_ENTITY
      result.json mustBe unprocessableResponse

      server.verify(
        getRequestedFor(urlEqualTo(accountActivityUrl))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withHeader("X-Message-Type", equalTo("ACCOUNT_ACTIVITY"))
      )

      verifyHipHeaders("GET", accountActivityUrl, expectedBody = None)
    }

    "returns a 500 response" in {
      val internalServerErrorResponse = Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal Server Error",
          "logId"   -> "123456789"
        )
      )

      stubFor(INTERNAL_SERVER_ERROR, internalServerErrorResponse)

      val result = connector.retrieveAccountActivity(AccountActivityRequest(fromDate, toDate), pillar2Id).futureValue

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe internalServerErrorResponse

      server.verify(
        getRequestedFor(urlEqualTo(accountActivityUrl))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withHeader("X-Message-Type", equalTo("ACCOUNT_ACTIVITY"))
      )

      verifyHipHeaders("GET", accountActivityUrl, expectedBody = None)
    }
  }
}
