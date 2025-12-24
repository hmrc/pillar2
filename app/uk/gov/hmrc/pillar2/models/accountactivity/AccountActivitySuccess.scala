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

package uk.gov.hmrc.pillar2.models.accountactivity

import play.api.libs.functional.syntax.given
import play.api.libs.json.*

import java.time.{LocalDate, LocalDateTime}

case class AccountActivitySuccess(processingDate: LocalDateTime, transactions: Seq[AccountActivityTransaction])

case class AccountActivityTransaction(
  transactionType:   String,
  transactionDesc:   String,
  startDate:         Option[LocalDate],
  endDate:           Option[LocalDate],
  accruedInterest:   Option[BigDecimal],
  chargeRefNo:       Option[String],
  transactionDate:   LocalDate,
  dueDate:           Option[LocalDate],
  originalAmount:    BigDecimal,
  outstandingAmount: Option[BigDecimal],
  clearedAmount:     Option[BigDecimal],
  standOverAmount:   Option[BigDecimal],
  appealFlag:        Option[Boolean],
  clearingDetails:   Option[Seq[AccountActivityClearance]]
)

case class AccountActivityClearance(
  transactionDesc: String,
  chargeRefNo:     Option[String],
  dueDate:         Option[LocalDate],
  amount:          BigDecimal,
  clearingDate:    LocalDate,
  clearingReason:  Option[String]
)

object AccountActivitySuccess {
  given Format[AccountActivitySuccess] = Format(
    (
      (__ \ "success" \ "processingDate").read[LocalDateTime] and
        (__ \ "success" \ "transactionDetails").read[Seq[AccountActivityTransaction]]
    )(AccountActivitySuccess.apply),
    (
      (__ \ "processingDate").write[LocalDateTime] and
        (__ \ "transactionDetails").write[Seq[AccountActivityTransaction]]
    )(s => (s.processingDate, s.transactions))
  )
}

object AccountActivityTransaction {
  given Format[AccountActivityTransaction] = Json.format
}

object AccountActivityClearance {
  given Format[AccountActivityClearance] = Json.format
}
