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
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus.{Fulfilled, Open}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType.{GlobeInformationReturn, Pillar2TaxReturn}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.SubmissionType.BTN
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions._

import java.time.{LocalDate, ZonedDateTime}

class ObligationsAndSubmissionsConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.obligations-and-submissions.port" -> server.port()
    )
    .build()

  lazy val connector: ObligationsAndSubmissionsConnector =
    app.injector.instanceOf[ObligationsAndSubmissionsConnector]

  val fromDate: LocalDate = LocalDate.now()
  val toDate:   LocalDate = LocalDate.now().plusYears(1)

  val url: String =
    s"/RESTAdapter/plr/obligations-and-submissions/?fromDate=${fromDate.toString}&toDate=${toDate.toString}"

  val response: ObligationsAndSubmissionsResponse = ObligationsAndSubmissionsResponse(
    ObligationsAndSubmissionsSuccessResponse(
      ZonedDateTime.now(),
      Seq(
        AccountingPeriodDetails(
          LocalDate.now(),
          LocalDate.now(),
          LocalDate.now(),
          underEnquiry = false,
          Seq(
            Obligation(
              Pillar2TaxReturn,
              Fulfilled,
              canAmend = false,
              Seq(
                Submission(
                  BTN,
                  ZonedDateTime.now(),
                  None
                )
              )
            ),
            Obligation(
              GlobeInformationReturn,
              Open,
              canAmend = true,
              Seq.empty
            )
          )
        )
      )
    )
  )

  "must return status as OK when obligations and submissions data is returned" in {
    server.stubFor(
      get(urlEqualTo(url))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.stringify(Json.toJson(response)))
        )
    )

    val result = connector.getObligationsAndSubmissions(fromDate, toDate).futureValue

    result mustBe response
  }

  "must return status as 500 when obligations and submissions data is not returned" in {
    server.stubFor(
      get(urlEqualTo(url))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .willReturn(
          aResponse()
            .withStatus(500)
            .withBody(Json.stringify(Json.obj()))
        )
    )

    val result = connector.getObligationsAndSubmissions(fromDate, toDate).failed
    result.failed.map { ex =>
      ex mustBe a[InternalServerException]
    }
  }
}
