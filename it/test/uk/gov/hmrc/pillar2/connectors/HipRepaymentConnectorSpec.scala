package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.Inspectors.forAll as forAllInspectors
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.{MimeTypes, HeaderNames as PlayHeaderNames}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.connectors.hip.HipRepaymentConnector
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.repayment.common.{BankDetails, RepaymentContactDetails, RepaymentDetails}
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail
import uk.gov.hmrc.pillar2.services.RepaymentService

class HipRepaymentConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with IntegrationPatience {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.hip.create-repayment.port" -> server.port(),
      "features.hip.create-payment"                     -> true
    )
    .build()

  lazy val connector: HipRepaymentConnector =
    app.injector.instanceOf[HipRepaymentConnector]

  lazy val service: RepaymentService =
    app.injector.instanceOf[RepaymentService]

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
        """
          |{
          |  "origin": "HoD",
          |  "response": {
          |    "error": {
          |      "code": "400",
          |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577",
          |      "message": "string"
          |    }
          |  }
          |}""".stripMargin

      stubRepaymentResponse(400, badRequestResponseBody)

      val response: HttpResponse = connector.sendRepaymentDetails(repaymentRequest).futureValue
      checkStandardErrorResponse(response, 400)
    }

    "handle 422 responses " in
      forAllInspectors(Seq("003", "004", "016")) { code =>
        val repaymentRequest = createRepaymentRequest()

        val badRequestMultipleErrorsBody =
          s"""
            |{
            |  "errors": {
            |    "code": "$code",
            |    "processingDate": "2022-01-31T09:26:17Z",
            |    "text": "error message"
            |  }
            |}""".stripMargin

        stubRepaymentResponse(422, badRequestMultipleErrorsBody)
        val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
        check422(response, code)

      }

    "handle INTERNAL_SERVER_ERROR response (500)" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456781",
        name = "Jane Smith",
        utr = Some("0987654321")
      )

      val internalServerErrorBody =
        """
          |{
          |  "origin": "HoD",
          |  "response": {
          |    "error": {
          |      "code": "500",
          |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577",
          |      "message": "string"
          |    }
          |  }
          |}""".stripMargin

      stubRepaymentResponse(500, internalServerErrorBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      checkStandardErrorResponse(response, 500)
    }

    "handle SERVICE_UNAVAILABLE response (503)" in {
      val repaymentRequest = createRepaymentRequest(
        plrReference = "XMPLR0123456789",
        name = "Alice Johnson",
        utr = Some("1122334455")
      )

      val serviceUnavailableBody =
        """{
          |  "origin": "HIP",
          |  "response": {
          |    "failures": [
          |      {
          |        "type": "string",
          |        "reason": "string"
          |      }
          |    ]
          |  }
          |}""".stripMargin

      stubRepaymentResponse(503, serviceUnavailableBody)

      val response = connector.sendRepaymentDetails(repaymentRequest).futureValue
      response.status mustBe 503
      (Json.parse(response.body) \ "response" \ "failures").as[List[Map[String, String]]].head mustBe Map(
        "type"   -> "string",
        "reason" -> "string"
      )
    }

    "must return UnexpectedResponse when the HTTP call fails" in {
      val repaymentRequest = createRepaymentRequest()

      stubFailedPostResponse("/RESTAdapter/plr/repayment")

      connector
        .sendRepaymentDetails(repaymentRequest)
        .failed
        .futureValue mustBe UnexpectedResponse
    }

  }

  private def checkStandardErrorResponse(response: HttpResponse, expectedStatus: Int) =
    assert(
      response.status === expectedStatus &&
        (Json.parse(response.body) \ "origin").isDefined &&
        (Json.parse(response.body) \ "response" \ "error" \ "code").as[String] === expectedStatus.toString
    )

  private def check422(response: HttpResponse, expectedCode: String) =
    assert(
      response.status === UNPROCESSABLE_ENTITY &&
        (Json.parse(response.body) \ "errors" \ "code").as[String] === expectedCode
    )

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
      post(urlEqualTo("/RESTAdapter/plr/repayment"))
        .withHeader("X-Originating-System", equalTo("PLR"))
        .withHeader("X-Transmitting-System", equalTo("HIP"))
        .withHeader(PlayHeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
