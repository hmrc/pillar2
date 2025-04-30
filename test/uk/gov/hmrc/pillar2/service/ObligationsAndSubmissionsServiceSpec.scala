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

package uk.gov.hmrc.pillar2.service

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.ApiInternalServerError
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus.{Fulfilled, Open}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType.{GIR, UKTR}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.SubmissionType.UKTR_CREATE
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions._

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class ObligationsAndSubmissionsServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service = new ObligationsAndSubmissionsService(mockObligationsAndSubmissionsConnector)

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
                  UKTR_CREATE,
                  ZonedDateTime.now,
                  None
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

  "getObligationsAndSubmissions" - {
    "should return successful response pillar2Id" in {
      when(
        mockObligationsAndSubmissionsConnector
          .getObligationsAndSubmissions(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      )
        .thenReturn(Future.successful(HttpResponse(OK, Json.toJson(response).toString())))

      val result = service.getObligationsAndSubmissions(fromDate, toDate).futureValue
      result mustBe response
    }

    "should throw a ApiInternalServerError" in {

      when(
        mockObligationsAndSubmissionsConnector
          .getObligationsAndSubmissions(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      )
        .thenReturn(Future.failed(ApiInternalServerError))

      val error: Throwable = service.getObligationsAndSubmissions(fromDate, toDate).failed.futureValue

      error mustBe ApiInternalServerError
    }
  }

}
