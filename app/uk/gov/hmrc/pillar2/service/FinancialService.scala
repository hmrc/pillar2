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
import uk.gov.hmrc.pillar2.service.FinancialService.*

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialService @Inject() (
  financialDataConnector: FinancialDataConnector
)(using
  ec: ExecutionContext
) extends Logging {

  def getTransactionHistory(plrReference: String, dateFrom: LocalDate, dateTo: LocalDate)(using
    hc:                                   HeaderCarrier
  ): Future[Either[FinancialDataError, TransactionHistory]] =
    retrieveCompleteFinancialDataResponse(plrReference, dateFrom, dateTo)
      .map { financialData =>
        val paymentData:   Seq[FinancialHistory] = getPaymentData(financialData)
        val repaymentData: Seq[FinancialHistory] = getRepaymentData(financialData)
        val interestData:  Seq[FinancialHistory] = getInterestData(financialData)

        if repaymentData.isEmpty && paymentData.isEmpty && interestData.isEmpty then {
          Left(FinancialDataError("NOT_FOUND", "No relevant financial data found"))
        } else {
          val sortedFinancialHistory: Seq[FinancialHistory] = (paymentData ++ repaymentData ++ interestData)
            .sortBy(_.paymentType)
            .sortBy(_.date)(Ordering[LocalDate].reverse)
          Right(TransactionHistory(plrReference, sortedFinancialHistory))
        }
      }
      .recover { case e: FinancialDataError =>
        logger.error(s"Error returned from getFinancials for plrReference=$plrReference - Error code=${e.code} Error reason=${e.reason}")
        Left(e)
      }

  def retrieveCompleteFinancialDataResponse(plrReference: String, dateFrom: LocalDate, dateTo: LocalDate)(using
    headerCarrier:                                        HeaderCarrier
  ): Future[FinancialDataResponse] = {

    val adjustedStartDate = {
      val sevenYearsAgo = dateTo.minusYears(7)
      if dateFrom.isBefore(sevenYearsAgo) then sevenYearsAgo else dateFrom
    }

    financialDataConnector.retrieveFinancialData(plrReference, dateFrom = adjustedStartDate, dateTo = dateTo)
  }

  private def getPaymentData(response: FinancialDataResponse): Seq[FinancialHistory] =
    for {
      financialData <- response.financialTransactions.filter(_.mainTransaction.contains(PaymentIdentifier))
      financialItem <- financialData.items.headOption
      dueDate       <- financialItem.dueDate
      paymentAmount <- financialItem.paymentAmount
    } yield FinancialHistory(date = dueDate, paymentType = Payment, amountPaid = paymentAmount.abs, amountRepaid = 0.00)

  private def getRepaymentData(response: FinancialDataResponse): Seq[FinancialHistory] =
    for {
      financialData  <- response.financialTransactions
      financialItems <- financialData.items.filter(_.clearingReason.contains(RepaymentReason))
      clearingDate   <- financialItems.clearingDate
      amount         <- financialItems.amount
    } yield FinancialHistory(date = clearingDate, paymentType = Repayment, amountPaid = 0.00, amountRepaid = amount.abs)

  private def getInterestData(response: FinancialDataResponse): Seq[FinancialHistory] =
    for {
      financialData  <- response.financialTransactions.filter(_.mainTransaction.contains(RepaymentInterestIdentifier))
      financialItems <- financialData.items
      clearingDate   <- financialItems.clearingDate
      amount         <- financialItems.amount
    } yield FinancialHistory(date = clearingDate, paymentType = RepaymentInterest, amountPaid = 0.00, amountRepaid = amount.abs)
}

object FinancialService {

  private type PaymentType              = String
  private type MainTransactionRefNumber = String

  private val Payment:           PaymentType = "Payment"
  private val Repayment:         PaymentType = "Repayment"
  private val RepaymentInterest: PaymentType = "Repayment interest"

  private val PaymentIdentifier:           MainTransactionRefNumber = "0060"
  private val RepaymentInterestIdentifier: MainTransactionRefNumber = "6504"

  private val RepaymentReason: String = "Outgoing payment - Paid"
}
