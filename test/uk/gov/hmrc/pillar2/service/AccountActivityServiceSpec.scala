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

package uk.gov.hmrc.pillar2.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.connectors.AccountActivityConnector
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.accountactivity.*
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.hip.{ApiFailure, ApiFailureResponse}

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import scala.concurrent.Future

class AccountActivityServiceSpec extends BaseSpec {

  trait AccountActivityServiceTestCase(httpStatus: Int, body: JsValue) {
    lazy val mockConnector: AccountActivityConnector = {
      val connector = mock[AccountActivityConnector]
      when(connector.retrieveAccountActivity(any(), any())(using any(), any()))
        .thenReturn(Future.successful(HttpResponse(httpStatus, Json.stringify(body))))
      connector
    }

    lazy val service: AccountActivityService = AccountActivityService(mockConnector)

    val request: AccountActivityRequest = AccountActivityRequest(
      fromDate = LocalDate.now().minusYears(1),
      toDate = LocalDate.now()
    )
  }

  "getAccountActivity" - {
    "should return ApiSuccessResponse for valid response (200)" in new AccountActivityServiceTestCase(
      OK,
      Json.parse(getClass.getResourceAsStream("/data/sample-account-activity.json"))
    ) {

      val result: AccountActivitySuccess = service.getAccountActivity(request, pillar2Id).futureValue

      result mustBe accountActivityJsonParsed
    }

    "should throw ValidationError for 422 response" in new AccountActivityServiceTestCase(
      UNPROCESSABLE_ENTITY,
      Json.toJson(ApiFailureResponse(ApiFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed")))
    ) {
      val error: ETMPValidationError = intercept[ETMPValidationError] {
        throw service.getAccountActivity(request, pillar2Id).failed.futureValue
      }
      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in new AccountActivityServiceTestCase(
      OK,
      Json.toJson("{invalid json}")
    ) {
      val error: InvalidJsonError = intercept[InvalidJsonError] {
        throw service.getAccountActivity(request, pillar2Id).failed.futureValue
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-201/422 responses" in new AccountActivityServiceTestCase(
      INTERNAL_SERVER_ERROR,
      Json.obj()
    ) {
      intercept[ApiInternalServerError.type] {
        throw service.getAccountActivity(request, pillar2Id).failed.futureValue
      }
    }
  }

  def accountActivityJsonParsed: AccountActivitySuccess = AccountActivitySuccess(
    processingDate = LocalDateTime.of(2001, 12, 17, 9, 30, 47, 0),
    Seq(
      AccountActivityTransaction(
        transactionType = "Payment",
        transactionDesc = "On Account Pillar 2 (Payment on Account)",
        startDate = None,
        endDate = None,
        accruedInterest = None,
        chargeRefNo = None,
        transactionDate = LocalDate.of(2025, 10, 15),
        dueDate = None,
        originalAmount = BigDecimal("10000"),
        outstandingAmount = Some(BigDecimal("1000")),
        clearedAmount = Some(BigDecimal("9000")),
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = Some(
          Seq(
            AccountActivityClearance(
              transactionDesc = "Pillar 2 UK Tax Return Pillar 2 DTT",
              chargeRefNo = Some("X123456789012"),
              dueDate = Some(LocalDate.of(2025, 12, 31)),
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Allocated to Charge")
            ),
            AccountActivityClearance(
              transactionDesc = "Pillar 2 UK Tax Return Pillar 2 MTT IIR",
              chargeRefNo = Some("X123456789012"),
              dueDate = Some(LocalDate.of(2025, 12, 31)),
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Allocated to Charge")
            ),
            AccountActivityClearance(
              transactionDesc = "Pillar 2 UK Tax Return Pillar 2 MTT UTPR",
              chargeRefNo = Some("X123456789012"),
              dueDate = Some(LocalDate.of(2025, 12, 31)),
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Allocated to Charge")
            ),
            AccountActivityClearance(
              transactionDesc = "Pillar 2 Discovery Assessment Pillar 2 DTT",
              chargeRefNo = Some("X123456789012"),
              dueDate = Some(LocalDate.of(2025, 12, 31)),
              amount = BigDecimal("3000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Allocated to Charge")
            )
          )
        )
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 UK Tax Return Pillar 2 DTT",
        startDate = Some(LocalDate.of(2025, 1, 1)),
        endDate = Some(LocalDate.of(2025, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("X123456789012"),
        transactionDate = LocalDate.of(2025, 2, 15),
        dueDate = Some(LocalDate.of(2025, 12, 31)),
        originalAmount = BigDecimal("2000"),
        outstandingAmount = None,
        clearedAmount = Some(BigDecimal("2000")),
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = Some(
          Seq(
            AccountActivityClearance(
              transactionDesc = "On Account Pillar 2 (Payment on Account)",
              chargeRefNo = None,
              dueDate = None,
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Cleared by Payment")
            )
          )
        )
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 UK Tax Return Pillar 2 MTT IIR",
        startDate = Some(LocalDate.of(2025, 1, 1)),
        endDate = Some(LocalDate.of(2025, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("X123456789012"),
        transactionDate = LocalDate.of(2025, 2, 15),
        dueDate = Some(LocalDate.of(2025, 12, 31)),
        originalAmount = BigDecimal("2000"),
        outstandingAmount = None,
        clearedAmount = Some(BigDecimal("2000")),
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = Some(
          Seq(
            AccountActivityClearance(
              transactionDesc = "On Account Pillar 2 (Payment on Account)",
              chargeRefNo = None,
              dueDate = None,
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Cleared by Payment")
            )
          )
        )
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 UK Tax Return Pillar 2 MTT UTPR",
        startDate = Some(LocalDate.of(2025, 1, 1)),
        endDate = Some(LocalDate.of(2025, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("X123456789012"),
        transactionDate = LocalDate.of(2025, 2, 15),
        dueDate = Some(LocalDate.of(2025, 12, 31)),
        originalAmount = BigDecimal("2000"),
        outstandingAmount = None,
        clearedAmount = Some(BigDecimal("2000")),
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = Some(
          Seq(
            AccountActivityClearance(
              transactionDesc = "On Account Pillar 2 (Payment on Account)",
              chargeRefNo = None,
              dueDate = None,
              amount = BigDecimal("2000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Cleared by Payment")
            )
          )
        )
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 Discovery Assessment Pillar 2 DTT",
        startDate = Some(LocalDate.of(2025, 1, 1)),
        endDate = Some(LocalDate.of(2025, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("XD23456789012"),
        transactionDate = LocalDate.of(2025, 2, 15),
        dueDate = Some(LocalDate.of(2025, 12, 31)),
        originalAmount = BigDecimal("3000"),
        outstandingAmount = None,
        clearedAmount = Some(BigDecimal("3000")),
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = Some(
          Seq(
            AccountActivityClearance(
              transactionDesc = "On Account Pillar 2 (Payment on Account)",
              chargeRefNo = None,
              dueDate = None,
              amount = BigDecimal("3000"),
              clearingDate = LocalDate.of(2025, 10, 15),
              clearingReason = Some("Cleared by Payment")
            )
          )
        )
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 Determination Pillar 2 MTT IIR",
        startDate = Some(LocalDate.of(2026, 1, 1)),
        endDate = Some(LocalDate.of(2026, 12, 31)),
        accruedInterest = Some(BigDecimal("35")),
        chargeRefNo = Some("XDT3456789012"),
        transactionDate = LocalDate.of(2027, 2, 15),
        dueDate = Some(LocalDate.of(2028, 3, 31)),
        originalAmount = BigDecimal("3100"),
        outstandingAmount = Some(BigDecimal("3100")),
        clearedAmount = None,
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = None
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 Overpaid Claim Assmt Pillar 2 MTT UTPR",
        startDate = Some(LocalDate.of(2026, 1, 1)),
        endDate = Some(LocalDate.of(2026, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("XOC3456789012"),
        transactionDate = LocalDate.of(2027, 2, 15),
        dueDate = Some(LocalDate.of(2028, 3, 31)),
        originalAmount = BigDecimal("4100"),
        outstandingAmount = Some(BigDecimal("4100")),
        clearedAmount = None,
        standOverAmount = Some(BigDecimal("4100")),
        appealFlag = Some(true),
        clearingDetails = None
      ),
      AccountActivityTransaction(
        transactionType = "Credit",
        transactionDesc = "Pillar 2 UKTR RPI Pillar 2 OECD RPI",
        startDate = None,
        endDate = None,
        accruedInterest = None,
        chargeRefNo = Some("XR23456789012"),
        transactionDate = LocalDate.of(2025, 3, 15),
        dueDate = None,
        originalAmount = BigDecimal("-100"),
        outstandingAmount = Some(BigDecimal("-100")),
        clearedAmount = None,
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = None
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 UKTR DTT LFP AUTO PEN",
        startDate = Some(LocalDate.of(2024, 1, 1)),
        endDate = Some(LocalDate.of(2024, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("XPN3456789012"),
        transactionDate = LocalDate.of(2026, 7, 1),
        dueDate = Some(LocalDate.of(2026, 7, 31)),
        originalAmount = BigDecimal("100"),
        outstandingAmount = Some(BigDecimal("100")),
        clearedAmount = None,
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = None
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 UKTR Interest Pillar 2 DTT Int",
        startDate = Some(LocalDate.of(2024, 1, 1)),
        endDate = Some(LocalDate.of(2024, 12, 31)),
        accruedInterest = None,
        chargeRefNo = Some("XIN3456789012"),
        transactionDate = LocalDate.of(2025, 10, 15),
        dueDate = Some(LocalDate.of(2025, 10, 15)),
        originalAmount = BigDecimal("35"),
        outstandingAmount = Some(BigDecimal("35")),
        clearedAmount = None,
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = None
      ),
      AccountActivityTransaction(
        transactionType = "Debit",
        transactionDesc = "Pillar 2 Record Keeping Pen TG PEN",
        startDate = None,
        endDate = None,
        accruedInterest = None,
        chargeRefNo = Some("XIN3456789012"),
        transactionDate = LocalDate.of(2026, 6, 30),
        dueDate = Some(LocalDate.of(2026, 7, 30)),
        originalAmount = BigDecimal("3500"),
        outstandingAmount = Some(BigDecimal("3500")),
        clearedAmount = None,
        standOverAmount = None,
        appealFlag = None,
        clearingDetails = None
      )
    )
  )
}
