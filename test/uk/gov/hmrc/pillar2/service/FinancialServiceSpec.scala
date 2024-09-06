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
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.FinancialDataError
import uk.gov.hmrc.pillar2.models.financial._
import uk.gov.hmrc.pillar2.service.FinancialService
import uk.gov.hmrc.pillar2.service.FinancialServiceSpec._

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FinancialServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  private val service = new FinancialService(mockFinancialDataConnector)

  "getPaymentHistory" - {
    "return payment history if relevant fields are defined with both payment and Refund sorted by date" in {
      when(mockFinancialDataConnector.retrieveFinancialData(any())(any(), any())).thenReturn(Future.successful(financialDataResponse))

      forAll(plrReferenceGen) { plrReference =>
        val result = await(service.getTransactionHistory(plrReference))
        result mustBe Right(paymentHistoryResult(plrReference))
      }
    }

    "return payment description first if Refund and payments are made on the same day" in {
      when(mockFinancialDataConnector.retrieveFinancialData(any())(any(), any())).thenReturn(Future.successful(financialDataResponseSameDay))

      val result = await(service.getTransactionHistory("123"))
      result mustBe Right(paymentHistoryResultSameDay("123"))
    }

    "return the error financial data responds" in {
      val result = FinancialDataError("NOT_FOUND", "not found")

      when(mockFinancialDataConnector.retrieveFinancialData(any())(any(), any()))
        .thenReturn(Future.failed(result))

      forAll(plrReferenceGen) { plrReference =>
        service.getTransactionHistory(plrReference).map { response =>
          response mustBe Left(result)
        }
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

  val paymentHistoryResult: String => TransactionHistory = (plrReference: String) =>
    TransactionHistory(
      plrReference,
      List(
        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 100.0, 0.00),
        FinancialHistory(LocalDate.now.plusDays(2), "Refund", 0.0, 100.0)
      )
    )

  val paymentHistoryResultSameDay: String => TransactionHistory = (plrReference: String) =>
    TransactionHistory(
      plrReference,
      List(
        FinancialHistory(LocalDate.now, "Payment", 100.0, 0.00),
        FinancialHistory(LocalDate.now, "Payment", 300.0, 0.00),
        FinancialHistory(LocalDate.now, "Refund", 0.0, 100.0),
        FinancialHistory(LocalDate.now, "Refund", 0.0, 111.0),
        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 200.0, 0.00),
        FinancialHistory(LocalDate.now.plusDays(1), "Refund", 0.0, 200.0)
      )
    )

}
