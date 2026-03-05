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

package uk.gov.hmrc.pillar2.models.hods.subscription.common

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class OriginalAccountingPeriod(
  taxObligationStartDate: LocalDate,
  taxObligationEndDate:   LocalDate
)

object OriginalAccountingPeriod {
  given format: OFormat[OriginalAccountingPeriod] = Json.format[OriginalAccountingPeriod]
}

final case class NewAccountingPeriodDetails(
  updateObligationStartDate: LocalDate,
  updateObligationEndDate:   LocalDate
)

object NewAccountingPeriodDetails {
  given format: OFormat[NewAccountingPeriodDetails] = Json.format[NewAccountingPeriodDetails]
}

final case class AccountingPeriodAmendV2(
  amendAccountingPeriod:     Boolean,
  originalAccountingPeriods: Option[Seq[OriginalAccountingPeriod]] = None,
  newAccountingPeriod:       Option[NewAccountingPeriodDetails] = None
)

object AccountingPeriodAmendV2 {
  given format: OFormat[AccountingPeriodAmendV2] = Json.format[AccountingPeriodAmendV2]
}
