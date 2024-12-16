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
import uk.gov.hmrc.pillar2.models.hods.repayment.common.BankDetails
import uk.gov.hmrc.pillar2.models.hods.repayment.common.RepaymentContactDetails
import uk.gov.hmrc.pillar2.models.hods.repayment.common.RepaymentDetails
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail

class RepaymentConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.create-repayment.port" -> server.port()
    )
    .build()

  lazy val connector: RepaymentConnector =
    app.injector.instanceOf[RepaymentConnector]

  "RepaymentConnector" - {

    "successfully send repayment details and receive success response" in {
      val repaymentRequest = createRepaymentRequest()

      val successfulRepaymentResponseBody =
        """{
          |  "success": {
          |    "processingDate": "2022-01-31T09:26:17Z"
          |  }
          |}""".stripMargin

      stubRepaymentResponse(201, successfulRepaymentResponseBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 201
      (Json.parse(response.body) \ "success" \ "processingDate").as[String] mustBe "2022-01-31T09:26:17Z"
    }

    "handle BAD_REQUEST response with single error code" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "INVALID_PLR",
        name = "",
        utr = Some("invalid_utr")
      )

      val badRequestResponseBody =
        """{
          |  "failures": [
          |    {
          |      "code": "INVALID_PAYLOAD",
          |      "reason": "Submission has not passed validation. Invalid Payload."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(400, badRequestResponseBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 400
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid Payload."
      )
    }

    "handle BAD_REQUEST response with multiple error codes" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "INVALID_PLR",
        name = "",
        utr = None
      )

      val badRequestMultipleErrorsBody =
        """{
          |  "failures": [
          |    {
          |      "code": "INVALID_CORRELATIONID",
          |      "reason": "Submission has not passed validation. Invalid Header CorrelationId."
          |    },
          |    {
          |      "code": "INVALID_PAYLOAD",
          |      "reason": "Submission has not passed validation. Invalid Payload."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(400, badRequestMultipleErrorsBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 400
      val failures = (Json.parse(response.body) \ "failures").as[List[Map[String, String]]]
      failures must contain allElementsOf (
        List(
          Map(
            "code"   -> "INVALID_CORRELATIONID",
            "reason" -> "Submission has not passed validation. Invalid Header CorrelationId."
          ),
          Map(
            "code"   -> "INVALID_PAYLOAD",
            "reason" -> "Submission has not passed validation. Invalid Payload."
          )
        )
      )
    }

    "handle CONFLICT response (409) for duplicate submission" in {
      val repaymentRequest = createRepaymentRequest()

      val conflictResponseBody =
        """{
          |  "failures": [
          |    {
          |      "code": "DUPLICATE_SUBMISSION",
          |      "reason": "The backend has indicated that the request is a duplicate submission."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(409, conflictResponseBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 409
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "DUPLICATE_SUBMISSION",
        "reason" -> "The backend has indicated that the request is a duplicate submission."
      )
    }

    "handle UNPROCESSABLE_ENTITY response (422) when request could not be processed" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "John Doe",
        utr = Some("1234567890")
      )

      val unprocessableEntityBody =
        """{
          |  "failures": [
          |    {
          |      "code": "REQUEST_NOT_PROCESSED",
          |      "reason": "The backend has indicated that the request could not be processed."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(422, unprocessableEntityBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 422
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "REQUEST_NOT_PROCESSED",
        "reason" -> "The backend has indicated that the request could not be processed."
      )
    }

    "handle INTERNAL_SERVER_ERROR response (500)" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "Jane Smith",
        utr = Some("0987654321")
      )

      val internalServerErrorBody =
        """{
          |  "failures": [
          |    {
          |      "code": "SERVER_ERROR",
          |      "reason": "IF is currently experiencing problems that require live service intervention."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(500, internalServerErrorBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 500
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "SERVER_ERROR",
        "reason" -> "IF is currently experiencing problems that require live service intervention."
      )
    }

    "handle SERVICE_UNAVAILABLE response (503)" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "Alice Johnson",
        utr = Some("1122334455")
      )

      val serviceUnavailableBody =
        """{
          |  "failures": [
          |    {
          |      "code": "SERVICE_UNAVAILABLE",
          |      "reason": "Dependent systems are currently not responding."
          |    }
          |  ]
          |}""".stripMargin

      stubRepaymentResponse(503, serviceUnavailableBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 503
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
    }

    "handle empty response body" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "Alice Johnson",
        utr = Some("1122334455")
      )

      stubRepaymentResponse(200, "")

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 200
      response.body mustBe empty
    }

    "handle malformed JSON response" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "Alice Johnson",
        utr = Some("1122334455")
      )

      stubRepaymentResponse(200, "{malformed json}")

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 200
      response.body mustBe "{malformed json}"
    }

  }

  private def createRepaymentRequest(
    plrReference: String = "XMPLR0123456789",
    name:         String = "John Doe",
    utr:          Option[String] = Some("1234567890")
  ): RepaymentRequestDetail = RepaymentRequestDetail(
    RepaymentDetails(
      plrReference = plrReference,
      name = name,
      utr = utr,
      reasonForRepayment = "Overpayment",
      refundAmount = 100.00
    ),
    BankDetails(
      nameOnBankAccount = "John Doe",
      bankName = "Bank of Test",
      sortCode = Some("123456"),
      accountNumber = Some("12345678"),
      iban = None,
      bic = None,
      countryCode = None
    ),
    contactDetails = RepaymentContactDetails("john.doe@example.com, 0123456789")
  )

  private def stubRepaymentResponse(
    status: Int,
    body:   String
  ) =
    server.stubFor(
      post(urlEqualTo("/pillar2/repayment"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
        )
    )
}
