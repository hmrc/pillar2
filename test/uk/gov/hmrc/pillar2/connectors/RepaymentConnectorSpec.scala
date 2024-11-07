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
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "John Doe",
          utr = Some("1234567890"),
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

      val successfulRepaymentResponseBody =
        """{
          |  "success": {
          |    "processingDate": "2022-01-31T09:26:17Z"
          |  }
          |}""".stripMargin

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(successfulRepaymentResponseBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 201
      (Json.parse(response.body) \ "success" \ "processingDate").as[String] mustBe "2022-01-31T09:26:17Z"
    }

    "handle BAD_REQUEST response with single error code" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "INVALID_PLR",
          name = "",
          utr = Some("invalid_utr"),
          reasonForRepayment = "",
          refundAmount = -100.00
        ),
        BankDetails(
          nameOnBankAccount = "",
          bankName = "",
          sortCode = Some("invalid_sc"),
          accountNumber = Some("invalid_acc"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("")
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

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody(badRequestResponseBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 400
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid Payload."
      )
    }

    "handle BAD_REQUEST response with multiple error codes" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "INVALID_PLR",
          name = "",
          utr = None,
          reasonForRepayment = "",
          refundAmount = -50.00
        ),
        BankDetails(
          nameOnBankAccount = "",
          bankName = "",
          sortCode = Some("invalid_sc"),
          accountNumber = Some("invalid_acc"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("")
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

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody(badRequestMultipleErrorsBody)
          )
      )

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
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "John Doe",
          utr = Some("1234567890"),
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

      val conflictResponseBody =
        """{
          |  "failures": [
          |    {
          |      "code": "DUPLICATE_SUBMISSION",
          |      "reason": "The backend has indicated that the request is a duplicate submission."
          |    }
          |  ]
          |}""".stripMargin

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(409)
              .withHeader("Content-Type", "application/json")
              .withBody(conflictResponseBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 409
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "DUPLICATE_SUBMISSION",
        "reason" -> "The backend has indicated that the request is a duplicate submission."
      )
    }

    "handle UNPROCESSABLE_ENTITY response (422) when request could not be processed" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "John Doe",
          utr = Some("1234567890"),
          reasonForRepayment = "Error correction",
          refundAmount = 150.00
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

      val unprocessableEntityBody =
        """{
          |  "failures": [
          |    {
          |      "code": "REQUEST_NOT_PROCESSED",
          |      "reason": "The backend has indicated that the request could not be processed."
          |    }
          |  ]
          |}""".stripMargin

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(422)
              .withHeader("Content-Type", "application/json")
              .withBody(unprocessableEntityBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 422
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "REQUEST_NOT_PROCESSED",
        "reason" -> "The backend has indicated that the request could not be processed."
      )
    }

    "handle INTERNAL_SERVER_ERROR response (500)" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "Jane Smith",
          utr = Some("0987654321"),
          reasonForRepayment = "Error correction",
          refundAmount = 150.00
        ),
        BankDetails(
          nameOnBankAccount = "Jane Smith",
          bankName = "Bank of Test",
          sortCode = Some("654321"),
          accountNumber = Some("87654321"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("jane.smith@example.com, 0987654321")
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

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody(internalServerErrorBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 500
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "SERVER_ERROR",
        "reason" -> "IF is currently experiencing problems that require live service intervention."
      )
    }

    "handle SERVICE_UNAVAILABLE response (503)" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "Alice Johnson",
          utr = Some("1122334455"),
          reasonForRepayment = "Refund adjustment",
          refundAmount = 200.00
        ),
        BankDetails(
          nameOnBankAccount = "Alice Johnson",
          bankName = "Springfield Bank",
          sortCode = Some("789012"),
          accountNumber = Some("23456789"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("alice.johnson@example.com, 0123456789")
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

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(503)
              .withHeader("Content-Type", "application/json")
              .withBody(serviceUnavailableBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 503
      (Json.parse(response.body) \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "code"   -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
    }

    "handle unexpected status codes gracefully" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "Bob Williams",
          utr = Some("5566778899"),
          reasonForRepayment = "Tax correction",
          refundAmount = 250.00
        ),
        BankDetails(
          nameOnBankAccount = "Bob Williams",
          bankName = "National Bank",
          sortCode = Some("345678"),
          accountNumber = Some("56789012"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("bob.williams@example.com, 0987654321")
      )

      val unexpectedStatusBody =
        """{
          |  "error": "I'm a teapot"
          |}""".stripMargin

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(418)
              .withHeader("Content-Type", "application/json")
              .withBody(unexpectedStatusBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 418
      (Json.parse(response.body) \ "error").as[String] mustBe "I'm a teapot"
    }

    "handle invalid JSON response body" in {
      val repaymentRequest = RepaymentRequestDetail(
        RepaymentDetails(
          plrReference = "XMPLR0123456789",
          name = "Charlie Brown",
          utr = Some("6677889900"),
          reasonForRepayment = "Refund adjustment",
          refundAmount = 300.00
        ),
        BankDetails(
          nameOnBankAccount = "Charlie Brown",
          bankName = "Peanuts Bank",
          sortCode = Some("654321"),
          accountNumber = Some("87654321"),
          iban = None,
          bic = None,
          countryCode = None
        ),
        contactDetails = RepaymentContactDetails("charlie.brown@example.com, 0123456789")
      )

      val invalidJsonResponseBody =
        """{
          |  "unexpectedField": "unexpectedValue"
          |}""".stripMargin

      server.stubFor(
        post(urlEqualTo("/pillar2/repayment"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidJsonResponseBody)
          )
      )

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 201
      // Depending on how RepaymentConnector handles the response,
      // you might want to parse the JSON and verify fields or handle exceptions.
      // Here, we're just verifying the unexpected fields are present.
      (Json.parse(response.body) \ "unexpectedField").as[String] mustBe "unexpectedValue"
    }
  }
}
