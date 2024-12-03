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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec

class UKTaxReturnConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.submit-uk-tax-return.port" -> server.port()
    )
    .build()

  lazy val connector: UKTaxReturnConnector =
    app.injector.instanceOf[UKTaxReturnConnector]

  private val samplePayload = Json.obj(
    "accountingPeriodFrom" -> "2024-08-14",
    "accountingPeriodTo" -> "2024-12-14",
    "qualifyingGroup" -> true,
    "obligationDTT" -> true,
    "obligationMTT" -> true,
    "electionUKGAAP" -> true,
    "liabilities" -> Json.obj(
      "returnType" -> "NIL_RETURN"
    )
  )

  private val pillar2Id = "XMPLR0000000012"

  "UKTaxReturnConnector" - {
    "successfully submit UK tax return and receive success response" in {
      val successResponse = Json.obj(
        "success" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z",
          "formBundleNumber" -> "123456789012345",
          "chargeReference" -> "XM002610011234"
        )
      )

      server.stubFor(
        post(urlEqualTo("/submit-uk-tax-return"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody(successResponse.toString())
          )
      )

      val result = connector.submitUKTaxReturn(samplePayload, pillar2Id).futureValue

      result match {
        case Right(response) =>
          (response \ "success" \ "formBundleNumber").as[String] mustBe "123456789012345"
        case Left(_) => fail("Expected Right but got Left")
      }
    }

    "handle BAD_REQUEST (400) response" in {
      val errorResponse = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code" -> "INVALID_PAYLOAD",
            "reason" -> "Submission has not passed validation. Invalid Payload."
          )
        )
      )

      server.stubFor(
        post(urlEqualTo("/submit-uk-tax-return"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withBody(errorResponse.toString())
          )
      )

      val result = connector.submitUKTaxReturn(samplePayload, pillar2Id).futureValue

      result.isLeft mustBe true
    }

    "handle UNPROCESSABLE_ENTITY (422) response" in {
      val errorResponse = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code" -> "REQUEST_NOT_PROCESSED",
            "reason" -> "The backend has indicated that the request could not be processed."
          )
        )
      )

      server.stubFor(
        post(urlEqualTo("/submit-uk-tax-return"))
          .willReturn(
            aResponse()
              .withStatus(422)
              .withBody(errorResponse.toString())
          )
      )

      val result = connector.submitUKTaxReturn(samplePayload, pillar2Id).futureValue

      result.isLeft mustBe true
    }

    "handle INTERNAL_SERVER_ERROR (500) response" in {
      val errorResponse = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code" -> "SERVER_ERROR",
            "reason" -> "IF is currently experiencing problems that require live service intervention."
          )
        )
      )

      server.stubFor(
        post(urlEqualTo("/submit-uk-tax-return"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody(errorResponse.toString())
          )
      )

      val result = connector.submitUKTaxReturn(samplePayload, pillar2Id).futureValue

      result.isLeft mustBe true
    }
  }
}
