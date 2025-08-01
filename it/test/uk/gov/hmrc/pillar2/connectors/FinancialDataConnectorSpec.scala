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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.connectors.FinancialDataConnectorSpec._
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.financial.{FinancialDataResponse, FinancialItem, FinancialTransaction}
import uk.gov.hmrc.pillar2.models.{FinancialDataError, FinancialDataErrorResponses}

import java.time.{LocalDate, LocalDateTime}

class FinancialDataConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.financial-data.port" -> server.port()
    )
    .build()

  lazy val connector: FinancialDataConnector =
    app.injector.instanceOf[FinancialDataConnector]

  val startDate: LocalDate = LocalDate.now()
  val endDate:   LocalDate = LocalDate.now().plusYears(1)
  val url: String =
    s"/enterprise/financial-data/ZPLR/$PlrReference/PLR?dateFrom=$startDate&dateTo=$endDate&onlyOpenItems=false&includeLocks=false&calculateAccruedInterest=true&customerPaymentInformation=true"

  "FinancialDataConnector" - {
    "must return status as OK when financial data is returned" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val result = await(connector.retrieveFinancialData(PlrReference, startDate, endDate))
      result mustBe response
    }

    "must return status as BAD_REQUEST" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withBody(Json.stringify(Json.parse(FinancialDataNotFound)))
          )
      )

      val result = connector.retrieveFinancialData(PlrReference, startDate, endDate).failed

      await(result) mustBe Json.parse(FinancialDataNotFound).as[FinancialDataError]
    }

    "must return status as INTERNAL_SERVER_ERROR" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withBody(Json.stringify(Json.parse(FinancialServerError)))
          )
      )

      val result = connector.retrieveFinancialData(PlrReference, startDate, endDate).failed

      await(result) mustBe Json.parse(FinancialServerError).as[FinancialDataError]
    }

    "handle multiple errors returned from financial data" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withBody(Json.stringify(Json.parse(MultipleErrors)))
          )
      )

      val result = connector.retrieveFinancialData(PlrReference, startDate, endDate).failed

      await(result) mustBe Json.parse(MultipleErrors).as[FinancialDataErrorResponses]
    }
  }
}

object FinancialDataConnectorSpec {

  val PlrReference = "XMPLR0123456789"

  val response: FinancialDataResponse = FinancialDataResponse(
    "ZPLR",
    "XPLR00000000001",
    "PLR",
    LocalDateTime.now(),
    Seq(
      FinancialTransaction(
        mainTransaction = Some("0600"),
        items = Seq(FinancialItem(Some(LocalDate.now()), Some(100.00), None, None, None))
      )
    )
  )

  val FinancialDataNotFound: String =
    """
      |{
      |   "code": "NOT_FOUND",
      |   "reason": "The remote endpoint has indicated that no data can be found"
      |}
      |
      |""".stripMargin

  val FinancialServerError: String =
    """
      |{
      |   "code": "SERVER_ERROR",
      |   "reason": "DES is currently experiencing problems that require live service intervention"
      |}
      |
      |""".stripMargin

  val MultipleErrors: String =
    """
      |{
      |  "failures": [
      |    {
      |      "code": "INVALID_IDTYPE",
      |      "reason": "Submission has not passed validation. Invalid parameter idType."
      |    },
      |    {
      |      "code": "INVALID_IDNUMBER",
      |      "reason": "Submission has not passed validation. Invalid parameter idNumber."
      |    }
      |  ]
      |}
      |""".stripMargin
}
