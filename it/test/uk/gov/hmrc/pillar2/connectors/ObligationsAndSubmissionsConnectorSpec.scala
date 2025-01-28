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
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus.{F, O}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType.{GlobeInformationReturn, Pillar2TaxReturn}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.SubmissionType.UKTR
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.{Obligation, ObligationsAndSubmissionsResponse, Submission}

import java.time.LocalDate

class ObligationsAndSubmissionsConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.obligatiobs-and-submissions.port" -> server.port()
    )
    .build()

  lazy val connector: ObligationsAndSubmissionsConnector =
    app.injector.instanceOf[ObligationsAndSubmissionsConnector]

  val url: String =
    "/RESTAdapter/plr/obligations-and-submissions" //TODO: Where does the PLR ref fit in and do we need to add query params for start and end dates?

  "ObligationsAndSubmissionsConnector" - {
    "must return status as OK when obligations and submissions data is returned" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val result = await(connector.getObligationsAndSubmissions(PlrReference))
      result mustBe response
    }
  }
}

object ObligationsAndSubmissionsConnectorSpec {

  val PlrReference = "XMPLR0123456789"

  val response: ObligationsAndSubmissionsResponse = ObligationsAndSubmissionsResponse(
    LocalDate.now(),
    LocalDate.now(),
    LocalDate.now(),
    Seq(
      Obligation(
        Pillar2TaxReturn,
        F,
        Seq(
          Submission(
            UKTR,
            LocalDate.now()
          )
        )
      ),
      Obligation(
        GlobeInformationReturn,
        O,
        Seq.empty
      )
    )
  )
}
