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

package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.connectors.ObligationConnector
import uk.gov.hmrc.pillar2.controllers.ObligationControllerSpec._
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.obligation.ObligationStatus.Fulfilled
import uk.gov.hmrc.pillar2.models.obligation.{ObligationInformation, ObligationType}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ObligationControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[ObligationConnector].toInstance(mockObligationConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "Obligation Controller" - {

    "return 200 with obligation data when connector returns us obligation information" in {
      when(mockObligationConnector.getObligations(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(obligationResponse))

      val request =
        FakeRequest(GET, routes.ObligationController.getObligation(PlrReference, dateFrom.toString, dateTo.toString).url)

      val result = route(application, request).value
      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(obligationResponse)
    }

    "return 404 when cannot find successful response" in {
      when(mockObligationConnector.getObligations(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(new Exception("wrong")))

      val request =
        FakeRequest(GET, routes.ObligationController.getObligation(PlrReference, dateFrom.toString, dateTo.toString).url)

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND
    }
  }

}

object ObligationControllerSpec {
  val PlrReference = "XMPLR0123456789"
  val dateFrom: LocalDate = LocalDate.now()
  val dateTo:   LocalDate = LocalDate.now().plusYears(2)
  val obligationResponse: ObligationInformation =
    ObligationInformation(ObligationType.UKTR, Fulfilled, LocalDate.now(), LocalDate.now(), LocalDate.now())
}
