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
import uk.gov.hmrc.pillar2.service.FinancialService._

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
  ): Future[Either[FinancialDataError, TransactionHistory]] = {

    val result = for {
      financialData <- retrieveCompleteFinancialDataResponse(plrReference, dateFrom, dateTo)
      repaymentData <- Future successful getRepaymentData(financialData)
      paymentData   <- Future successful getPaymentData(financialData)
      sortedFinancialHistory = (paymentData ++ repaymentData).sortBy(_.paymentType).sortBy(_.date)(Ordering[LocalDate].reverse)
    } yield Right(TransactionHistory(plrReference, sortedFinancialHistory))

    result.recover { case e: FinancialDataError =>
      logger.error(s"Error returned from getFinancials for plrReference=$plrReference - Error code=${e.code} Error reason=${e.reason}")
      Left(e)
    }
  }

  private def retrieveCompleteFinancialDataResponse(plrReference: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit
    headerCarrier:                                                HeaderCarrier
  ): Future[FinancialDataResponse] =
    Future
      .sequence(
        splitIntoYearIntervals(dateFrom, dateTo).map(year => financialDataConnector.retrieveFinancialData(plrReference, year.startDate, year.endDate))
      )
      .map { financialDataResponses =>
        val allTransactions = financialDataResponses.flatMap(_.financialTransactions)

        FinancialDataResponse(
          idType = financialDataResponses.head.idType,
          idNumber = financialDataResponses.head.idNumber,
          regimeType = financialDataResponses.head.regimeType,
          processingDate = financialDataResponses.head.processingDate,
          financialTransactions = allTransactions
        )
      }

  private[service] def splitIntoYearIntervals(startDate: LocalDate, endDate: LocalDate): List[Years] = {

    val adjustedStartDate = if (startDate.isBefore(endDate.minusYears(7))) endDate.minusYears(7) else startDate

    val years = Iterator
      .iterate(adjustedStartDate)(_.plusYears(1))
      .takeWhile(_.isBefore(endDate))
      .toList

    years.foldLeft(List.empty[Years]) { (acc, currentStartDate) =>
      val currentEndDate =
        if (currentStartDate.plusYears(1).isBefore(endDate))
          currentStartDate.plusYears(1).minusDays(1)
        else
          endDate

      acc :+ Years(currentStartDate, currentEndDate)
    }
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
