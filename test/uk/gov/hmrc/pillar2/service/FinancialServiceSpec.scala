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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers.await
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.FinancialDataError
import uk.gov.hmrc.pillar2.models.financial._
import uk.gov.hmrc.pillar2.service.FinancialServiceSpec._

import java.time.LocalDate
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
class FinancialServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  private val service = new FinancialService(mockFinancialDataConnector)

  val startDate: LocalDate = LocalDate.now()
  val endDate:   LocalDate = LocalDate.now().plusDays(364)

  ".getPaymentHistory" - {
    "return payment history if relevant fields are defined with both payment and Repayment sorted by date" in {
      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponse))

      forAll(plrReferenceGen) { plrReference =>
        val result = await(service.getTransactionHistory(plrReference, startDate, endDate))
        result mustBe Right(paymentHistoryResult(plrReference))
      }
    }

    "return payment a not found error if no relevant history is found" in {
      val result = FinancialDataError("NOT_FOUND", "No relevant financial data found")
      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialResponseWithNeitherPaymentAndRepayment))

      forAll(plrReferenceGen) { plrReference =>
        val response = await(service.getTransactionHistory(plrReference, startDate, endDate))
        response mustBe Left(result)
      }
    }

    "return payment history when the response contains both payment and repayment" in {
      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialResponseWithPaymentAndRepayment))

      forAll(plrReferenceGen) { plrReference =>
        val result = await(service.getTransactionHistory(plrReference, startDate, endDate))
        result mustBe Right(paymentHistoryResult(plrReference))
      }
    }

    "return payment description first if Repayment and payments are made on the same day" in {
      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponseSameDay))

      val result = await(service.getTransactionHistory(plrReference = "123", startDate, endDate))
      result mustBe Right(paymentHistoryResultSameDay("123"))
    }

    "return the error financial data responds" in {
      val result = FinancialDataError("NOT_FOUND", "not found")

      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.failed(result))

      forAll(plrReferenceGen) { plrReference =>
        service.getTransactionHistory(plrReference, startDate, endDate).map { response =>
          response mustBe Left(result)
        }
      }
    }

    "truncate dateFrom to last 7 years if more than 7 years are requested" in {
      val startDate = LocalDate.now().minusYears(8)
      val endDate   = LocalDate.now()

      val sevenYearsBeforeEndDate = LocalDate.now().minusYears(7)

      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponse))

      forAll(plrReferenceGen) { plrReference =>
        await(service.getTransactionHistory(plrReference, startDate, endDate))
        verify(mockFinancialDataConnector, times(1))
          .retrieveFinancialData(eqTo(plrReference), eqTo(sevenYearsBeforeEndDate), eqTo(endDate))(any[HeaderCarrier](), any[ExecutionContext]())
      }
    }

    "should use original dateFrom if it is within the last seven years" in {
      val startDate = LocalDate.now().minusYears(6)
      val endDate   = LocalDate.now()

      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponse))

      forAll(plrReferenceGen) { plrReference =>
        await(service.getTransactionHistory(plrReference, startDate, endDate))
        verify(mockFinancialDataConnector, times(1))
          .retrieveFinancialData(eqTo(plrReference), eqTo(startDate), eqTo(endDate))(any[HeaderCarrier](), any[ExecutionContext]())
      }
    }
  }

  ".retrieveCompleteFinancialDataResponse" - {
    "return financial data response when successful" in {
      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponse))

      val result = await(service.retrieveCompleteFinancialDataResponse(pillar2Id, startDate, endDate))
      result mustBe financialDataResponse
    }

    "truncate dateFrom to last 7 years if more than 7 years are requested" in {
      val startDate               = LocalDate.now().minusYears(8)
      val endDate                 = LocalDate.now()
      val sevenYearsBeforeEndDate = LocalDate.now().minusYears(7)

      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(financialDataResponse))

      await(service.retrieveCompleteFinancialDataResponse(pillar2Id, startDate, endDate))
      verify(mockFinancialDataConnector, times(1))
        .retrieveFinancialData(eqTo(pillar2Id), eqTo(sevenYearsBeforeEndDate), eqTo(endDate))(any[HeaderCarrier](), any[ExecutionContext]())
    }

    "propagate financial data errors" in {
      val error = FinancialDataError("NOT_FOUND", "not found")

      when(
        mockFinancialDataConnector
          .retrieveFinancialData(any[String](), any[LocalDate](), any[LocalDate]())(any[HeaderCarrier](), any[ExecutionContext]())
      )
        .thenReturn(Future.failed(error))

      val future = service.retrieveCompleteFinancialDataResponse(pillar2Id, startDate, endDate)
      whenReady(future.failed) { exception =>
        exception mustBe error
      }
    }
  }

}

object FinancialServiceSpec {
  val financialDataResponse: FinancialDataResponse = FinancialDataResponse(
    idType = "ZPLR",
    idNumber = "XPLR00000000001",
    regimeType = "PLR",
    processingDate = LocalDateTime.now(),
    financialTransactions = Seq(
      FinancialTransaction(
        mainTransaction = Some("0060"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now().plusDays(1)),
            amount = None,
            paymentAmount = Some(100.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0000"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now().plusDays(3)),
            amount = None,
            paymentAmount = Some(100.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0100"),
        items = Seq(
          FinancialItem(
            dueDate = None,
            amount = Some(100.00),
            paymentAmount = None,
            clearingDate = Some(LocalDate.now().plusDays(2)),
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0100"),
        items = Seq(
          FinancialItem(
            dueDate = None,
            amount = Some(100.00),
            paymentAmount = None,
            clearingDate = None,
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      )
    )
  )

  val financialDataResponseSameDay: FinancialDataResponse = FinancialDataResponse(
    idType = "ZPLR",
    idNumber = "XPLR00000000001",
    regimeType = "PLR",
    processingDate = LocalDateTime.now(),
    financialTransactions = Seq(
      FinancialTransaction(
        mainTransaction = Some("0100"),
        items = Seq(
          FinancialItem(
            dueDate = None,
            amount = Some(200.00),
            paymentAmount = None,
            clearingDate = Some(LocalDate.now().plusDays(1)),
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0060"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now()),
            amount = None,
            paymentAmount = Some(100.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0060"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now()),
            amount = None,
            paymentAmount = Some(300.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0060"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now().plusDays(1)),
            amount = None,
            paymentAmount = Some(200.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0100"),
        items = Seq(
          FinancialItem(
            dueDate = None,
            amount = Some(100.00),
            paymentAmount = None,
            clearingDate = Some(LocalDate.now()),
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      ),
      FinancialTransaction(
        mainTransaction = Some("0100"),
        items = Seq(
          FinancialItem(
            dueDate = None,
            amount = Some(111.00),
            paymentAmount = None,
            clearingDate = Some(LocalDate.now()),
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      )
    )
  )

  val financialResponseWithPaymentAndRepayment: FinancialDataResponse = FinancialDataResponse(
    idType = "ZPLR",
    idNumber = "XPLR00000000001",
    regimeType = "PLR",
    processingDate = LocalDateTime.now(),
    financialTransactions = Seq(
      FinancialTransaction(
        mainTransaction = Some("0060"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now().plusDays(1)),
            amount = Some(-100.00),
            paymentAmount = Some(100.00),
            clearingDate = Some(LocalDate.now().plusDays(2)),
            clearingReason = Some("Outgoing payment - Paid")
          )
        )
      )
    )
  )

  val financialResponseWithNeitherPaymentAndRepayment: FinancialDataResponse = FinancialDataResponse(
    idType = "ZPLR",
    idNumber = "XPLR00000000001",
    regimeType = "PLR",
    processingDate = LocalDateTime.now(),
    financialTransactions = Seq(
      FinancialTransaction(
        mainTransaction = Some("0000"),
        items = Seq(
          FinancialItem(
            dueDate = Some(LocalDate.now().plusDays(3)),
            amount = None,
            paymentAmount = Some(100.00),
            clearingDate = None,
            clearingReason = None
          )
        )
      )
    )
  )

  val paymentHistoryResult: String => TransactionHistory = (plrReference: String) =>
    TransactionHistory(
      plrReference,
      List(
        FinancialHistory(LocalDate.now.plusDays(2), "Repayment", 0.0, 100.0),
        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 100.0, 0.00)
      )
    )

  val paymentHistoryResultSameDay: String => TransactionHistory = (plrReference: String) =>
    TransactionHistory(
      plrReference,
      List(
        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 200.0, 0.00),
        FinancialHistory(LocalDate.now.plusDays(1), "Repayment", 0.0, 200.0),
        FinancialHistory(LocalDate.now, "Payment", 100.0, 0.00),
        FinancialHistory(LocalDate.now, "Payment", 300.0, 0.00),
        FinancialHistory(LocalDate.now, "Repayment", 0.0, 100.0),
        FinancialHistory(LocalDate.now, "Repayment", 0.0, 111.0)
      )
    )

}
