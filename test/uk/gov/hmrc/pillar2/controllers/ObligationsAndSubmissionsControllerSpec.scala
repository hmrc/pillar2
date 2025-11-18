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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.ApiInternalServerError
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus.{Fulfilled, Open}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType.{GIR, UKTR}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.SubmissionType.ORN_CREATE
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions._
import uk.gov.hmrc.pillar2.service.ObligationsAndSubmissionsService

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID
import scala.concurrent.Future

class ObligationsAndSubmissionsControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction],
      bind[ObligationsAndSubmissionsService].to(mockObligationsAndSubmissionsService)
    )
    .build()

  override def afterEach(): Unit = {
    reset(mockObligationsAndSubmissionsService)
    reset(mockAuthConnector)
    super.afterEach()
  }

  val fromDate: LocalDate = LocalDate.now()
  val toDate:   LocalDate = LocalDate.now().plusYears(1)
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
              UKTR,
              Fulfilled,
              canAmend = false,
              Seq(
                Submission(
                  ORN_CREATE,
                  ZonedDateTime.now(),
                  Some("Country details")
                )
              )
            ),
            Obligation(
              GIR,
              Open,
              canAmend = true,
              Seq.empty
            )
          )
        )
      )
    )
  )

  "ObligationsAndSubmissionsController" - {

    "should return OK with obligations and submissions when data is found for pillar2Id" in {

      when(
        mockObligationsAndSubmissionsService.getObligationsAndSubmissions(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      )
        .thenReturn(Future.successful(response))

      val request =
        FakeRequest(GET, routes.ObligationsAndSubmissionsController.getObligationsAndSubmissions(fromDate.toString, toDate.toString).url)
          .withHeaders(
            "correlationid"         -> UUID.randomUUID().toString,
            "X-Transmitting-System" -> "HIP",
            "X-Originating-System"  -> "MDTP",
            "X-Receipt-Date"        -> ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            "X-Pillar2-Id"          -> pillar2Id
          )

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(response.success)
    }

    "should handle ApiInternalServerError from service" in {
      when(
        mockObligationsAndSubmissionsService.getObligationsAndSubmissions(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      )
        .thenReturn(Future.failed(ApiInternalServerError))

      val request =
        FakeRequest(GET, routes.ObligationsAndSubmissionsController.getObligationsAndSubmissions(fromDate.toString, toDate.toString).url)
          .withHeaders(
            "correlationid"         -> UUID.randomUUID().toString,
            "X-Transmitting-System" -> "HIP",
            "X-Originating-System"  -> "MDTP",
            "X-Receipt-Date"        -> ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            "X-Pillar2-Id"          -> pillar2Id
          )

      val result = route(application, request).value.failed.futureValue
      result mustEqual ApiInternalServerError
    }
  }

}
