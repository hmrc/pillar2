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

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.connectors.FinancialDataConnector
import uk.gov.hmrc.pillar2.models.FinancialDataError
import uk.gov.hmrc.pillar2.models.financial.{FinancialDataResponse, FinancialHistory, TransactionHistory}
import uk.gov.hmrc.pillar2.service.FinancialService.{PAYMENT_IDENTIFIER, Payment, REPAYMENT_IDENTIFIER, Refund, Years}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialService @Inject() (
  financialDataConnector: FinancialDataConnector
)(implicit
  ec: ExecutionContext
) extends Logging {

  def getTransactionHistory(plrReference: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit
    hc:                                   HeaderCarrier
  ): Future[Either[FinancialDataError, TransactionHistory]] =
    retrieveCompleteFinancialDataResponse(plrReference, dateFrom, dateTo)
      .map { financialData =>
        val repaymentData = getRepaymentData(financialData)
        val paymentData   = getPaymentData(financialData)

        if (repaymentData.isEmpty && paymentData.isEmpty) {
          Left(FinancialDataError("NOT_FOUND", "No relevant financial data found"))
        } else {
          val sortedFinancialHistory = (paymentData ++ repaymentData)
            .sortBy(_.paymentType)
            .sortBy(_.date)(Ordering[LocalDate].reverse)
          Right(TransactionHistory(plrReference, sortedFinancialHistory))
        }
      }
      .recover { case e: FinancialDataError =>
        logger.error(s"Error returned from getFinancials for plrReference=$plrReference - Error code=${e.code} Error reason=${e.reason}")
        Left(e)
      }

  private def retrieveCompleteFinancialDataResponse(plrReference: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit
    headerCarrier:                                                HeaderCarrier
  ): Future[FinancialDataResponse] = {

    val adjustedStartDate = {
      val sevenYearsAgo = dateTo.minusYears(7)
      if (dateFrom.isBefore(sevenYearsAgo)) sevenYearsAgo else dateFrom
    }

    financialDataConnector.retrieveFinancialData(plrReference, dateFrom = adjustedStartDate, dateTo = dateTo)
  }

  private def getPaymentData(response: FinancialDataResponse): Seq[FinancialHistory] =
    for {
      financialData  <- response.financialTransactions.filter(_.mainTransaction.contains(PAYMENT_IDENTIFIER))
      financialItems <- financialData.items
      dueDate        <- financialItems.dueDate
      paymentAmount  <- financialItems.paymentAmount
    } yield FinancialHistory(date = dueDate, paymentType = Payment, amountPaid = paymentAmount.abs, amountRepaid = 0.00)

  private def getRepaymentData(response: FinancialDataResponse): Seq[FinancialHistory] =
    for {
      financialData  <- response.financialTransactions
      financialItems <- financialData.items.filter(_.clearingReason.contains(REPAYMENT_IDENTIFIER))
      clearingDate   <- financialItems.clearingDate
      amount         <- financialItems.amount
    } yield FinancialHistory(date = clearingDate, paymentType = Refund, amountPaid = 0.00, amountRepaid = amount.abs)

}

object FinancialService {

  val Payment              = "Payment"
  val Refund               = "Refund"
  val PAYMENT_IDENTIFIER   = "0060"
  val REPAYMENT_IDENTIFIER = "Outgoing payment - Paid"

  private[service] case class Years(startDate: LocalDate, endDate: LocalDate)
}
