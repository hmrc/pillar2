/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.models.obligationsAndSubmissions

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.{LocalDate, ZonedDateTime}

class ObligationsAndSubmissionsSuccessResponseSpec extends AnyFreeSpec with Matchers {

  "ObligationsAndSubmissionsSuccessResponse" - {
    "must deserialize from valid JSON" in {
      val processingDate = ZonedDateTime.now()
      val accountingPeriodDetails = Seq(
        AccountingPeriodDetails(
          startDate = LocalDate.now(),
          endDate = LocalDate.now().plusMonths(3),
          dueDate = LocalDate.now().plusMonths(4),
          underEnquiry = false,
          obligations = Seq(
            Obligation(
              obligationType = ObligationType.UKTR,
              status = ObligationStatus.Fulfilled,
              canAmend = false,
              submissions = Seq(
                Submission(submissionType = SubmissionType.UKTR_CREATE, receivedDate = ZonedDateTime.now(), country = None)
              )
            )
          )
        )
      )

      val response = ObligationsAndSubmissionsSuccessResponse(
        processingDate = processingDate,
        accountingPeriodDetails = accountingPeriodDetails
      )

      val json = Json.obj(
        "processingDate" -> processingDate,
        "accountingPeriodDetails" -> Json.arr(
          Json.obj(
            "startDate"    -> accountingPeriodDetails.head.startDate,
            "endDate"      -> accountingPeriodDetails.head.endDate,
            "dueDate"      -> accountingPeriodDetails.head.dueDate,
            "underEnquiry" -> accountingPeriodDetails.head.underEnquiry,
            "obligations" -> Json.arr(
              Json.obj(
                "obligationType" -> "UKTR",
                "status"         -> "Fulfilled",
                "canAmend"       -> false,
                "submissions" -> Json.arr(
                  Json.obj(
                    "submissionType" -> "UKTR_CREATE",
                    "receivedDate"   -> accountingPeriodDetails.head.obligations.head.submissions.head.receivedDate
                  )
                )
              )
            )
          )
        )
      )

      val result = json.validate[ObligationsAndSubmissionsSuccessResponse]
      result mustEqual JsSuccess(response)
    }

    "must deserialize from JSON with empty submissions array" in {
      val processingDate = ZonedDateTime.now()
      val accountingPeriodDetails = Seq(
        AccountingPeriodDetails(
          startDate = LocalDate.now(),
          endDate = LocalDate.now().plusMonths(3),
          dueDate = LocalDate.now().plusMonths(4),
          underEnquiry = false,
          obligations = Seq(
            Obligation(
              obligationType = ObligationType.UKTR,
              status = ObligationStatus.Open,
              canAmend = true,
              submissions = Seq.empty
            )
          )
        )
      )

      val response = ObligationsAndSubmissionsSuccessResponse(
        processingDate = processingDate,
        accountingPeriodDetails = accountingPeriodDetails
      )

      val json = Json.obj(
        "processingDate" -> processingDate,
        "accountingPeriodDetails" -> Json.arr(
          Json.obj(
            "startDate"    -> accountingPeriodDetails.head.startDate,
            "endDate"      -> accountingPeriodDetails.head.endDate,
            "dueDate"      -> accountingPeriodDetails.head.dueDate,
            "underEnquiry" -> accountingPeriodDetails.head.underEnquiry,
            "obligations" -> Json.arr(
              Json.obj(
                "obligationType" -> "UKTR",
                "status"         -> "Open",
                "canAmend"       -> true,
                "submissions"    -> Json.arr()
              )
            )
          )
        )
      )

      val result = json.validate[ObligationsAndSubmissionsSuccessResponse]
      result mustEqual JsSuccess(response)
    }

    "must deserialize from JSON when submissions field is missing" in {
      val processingDate = ZonedDateTime.now()

      val accountingPeriodDetails = Seq(
        AccountingPeriodDetails(
          startDate = LocalDate.now(),
          endDate = LocalDate.now().plusMonths(3),
          dueDate = LocalDate.now().plusMonths(4),
          underEnquiry = false,
          obligations = Seq(
            Obligation(
              obligationType = ObligationType.UKTR,
              status = ObligationStatus.Open,
              canAmend = true,
              submissions = Seq.empty
            )
          )
        )
      )

      val response = ObligationsAndSubmissionsSuccessResponse(
        processingDate = processingDate,
        accountingPeriodDetails = accountingPeriodDetails
      )

      val json = Json.obj(
        "processingDate" -> processingDate,
        "accountingPeriodDetails" -> Json.arr(
          Json.obj(
            "startDate"    -> accountingPeriodDetails.head.startDate,
            "endDate"      -> accountingPeriodDetails.head.endDate,
            "dueDate"      -> accountingPeriodDetails.head.dueDate,
            "underEnquiry" -> false,
            "obligations" -> Json.arr(
              Json.obj(
                "obligationType" -> "UKTR",
                "status"         -> "Open",
                "canAmend"       -> true
                // submissions field intentionally omitted
              )
            )
          )
        )
      )

      val result = json.validate[ObligationsAndSubmissionsSuccessResponse]
      result mustEqual JsSuccess(response)
    }

    "must fail to deserialize from invalid JSON" in {
      val invalidJson = Json.obj(
        "processingDate"          -> "invalid-date",
        "accountingPeriodDetails" -> Json.arr()
      )

      val result = invalidJson.validate[ObligationsAndSubmissionsSuccessResponse]
      result.isError mustBe true
    }

    "must serialize to JSON" in {
      val processingDate = ZonedDateTime.now()
      val response = ObligationsAndSubmissionsSuccessResponse(
        processingDate = processingDate,
        accountingPeriodDetails = Seq.empty
      )

      val expectedJson = Json.obj(
        "processingDate"          -> processingDate,
        "accountingPeriodDetails" -> Json.arr()
      )

      val result = Json.toJson(response)
      result mustEqual expectedJson
    }
  }
}
