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
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.{LiabilityNilReturn, ReturnType, UKTRSubmissionNilReturn}

import java.time.LocalDate

class UKTaxReturnConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with IntegrationPatience {

  override lazy val app: Application =
    applicationBuilder()
      .configure(
        "microservice.services.submit-uk-tax-return.port" -> server.port()
      )
      .build()

  private lazy val connector =
    app.injector.instanceOf[UKTaxReturnConnector]

  private val submissionPayload = UKTRSubmissionNilReturn(
    accountingPeriodFrom = LocalDate.parse("2024-08-14"),
    accountingPeriodTo = LocalDate.parse("2024-12-14"),
    obligationMTT = true,
    electionUKGAAP = true,
    liabilities = LiabilityNilReturn(
      returnType = ReturnType.NIL_RETURN
    )
  )

  private val etmpUKTRUrl = "/RESTAdapter/PLR/UKTaxReturn"

  "UKTaxReturnConnector" - {
    "submit UK tax return" - {
      "successfully submit UK tax return with X-PILLAR2-Id and receive success response" in {
        val successResponse = Json.obj(
          "success" -> Json.obj(
            "processingDate"   -> "2024-03-14T09:26:17Z",
            "formBundleNumber" -> "123456789012345",
            "chargeReference"  -> "123456789012345"
          )
        )

        server.stubFor(
          post(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(CREATED)
                .withHeader("Content-Type", "application/json")
                .withBody(successResponse.toString())
            )
        )

        val result = connector.submitUKTaxReturn(submissionPayload).futureValue

        result.status mustBe CREATED
        result.json mustBe successResponse
        server.verify(
          postRequestedFor(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
        )
      }

      "handle BAD_REQUEST (400) response" in {
        val errorResponse = Json.obj(
          "error" -> Json.obj(
            "code"    -> "400",
            "message" -> "Bad Request",
            "logId"   -> "123456789"
          )
        )

        server.stubFor(
          post(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.submitUKTaxReturn(submissionPayload).futureValue
        result.status mustBe BAD_REQUEST
        result.json mustBe errorResponse
      }

      "handle UNPROCESSABLE_ENTITY (422) response" in {
        val errorResponse = Json.obj(
          "errors" -> Json.obj(
            "processingDate" -> "2024-03-14T09:26:17Z",
            "code"           -> "003",
            "text"           -> "Request could not be processed"
          )
        )

        server.stubFor(
          post(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(UNPROCESSABLE_ENTITY)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.submitUKTaxReturn(submissionPayload).futureValue
        result.status mustBe UNPROCESSABLE_ENTITY
        result.json mustBe errorResponse
      }

      "handle INTERNAL_SERVER_ERROR (500) response" in {
        val errorResponse = Json.obj(
          "error" -> Json.obj(
            "code"    -> "500",
            "message" -> "Internal Server Error",
            "logId"   -> "123456789"
          )
        )

        server.stubFor(
          post(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.submitUKTaxReturn(submissionPayload).futureValue
        result.status mustBe INTERNAL_SERVER_ERROR
        result.json mustBe errorResponse
      }
    }

    "amend UK tax return" - {
      "successfully amend UK tax return with X-PILLAR2-Id and receive success response" in {
        val successResponse = Json.obj(
          "success" -> Json.obj(
            "processingDate"   -> "2024-03-14T09:26:17Z",
            "formBundleNumber" -> "123456789012345",
            "chargeReference"  -> "123456789012345"
          )
        )

        server.stubFor(
          put(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withHeader("Content-Type", "application/json")
                .withBody(successResponse.toString())
            )
        )

        val result = connector.amendUKTaxReturn(submissionPayload).futureValue
        result.status mustBe OK
        result.json mustBe successResponse
      }

      "handle BAD_REQUEST (400) response" in {
        val errorResponse = Json.obj(
          "error" -> Json.obj(
            "code"    -> "400",
            "message" -> "Bad Request",
            "logId"   -> "123456789"
          )
        )

        server.stubFor(
          put(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.amendUKTaxReturn(submissionPayload).futureValue
        result.status mustBe BAD_REQUEST
        result.json mustBe errorResponse
      }

      "handle UNPROCESSABLE_ENTITY (422) response" in {
        val errorResponse = Json.obj(
          "errors" -> Json.obj(
            "processingDate" -> "2024-03-14T09:26:17Z",
            "code"           -> "003",
            "text"           -> "Request could not be processed"
          )
        )

        server.stubFor(
          put(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(UNPROCESSABLE_ENTITY)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.amendUKTaxReturn(submissionPayload).futureValue
        result.status mustBe UNPROCESSABLE_ENTITY
        result.json mustBe errorResponse
      }

      "handle INTERNAL_SERVER_ERROR (500) response" in {
        val errorResponse = Json.obj(
          "error" -> Json.obj(
            "code"    -> "500",
            "message" -> "Internal Server Error",
            "logId"   -> "123456789"
          )
        )

        server.stubFor(
          put(urlEqualTo(etmpUKTRUrl))
            .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
            .withRequestBody(equalToJson(Json.toJson(submissionPayload).toString()))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponse.toString())
            )
        )

        val result = connector.amendUKTaxReturn(submissionPayload).futureValue
        result.status mustBe INTERNAL_SERVER_ERROR
        result.json mustBe errorResponse
      }
    }
  }
}
