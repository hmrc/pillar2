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

package uk.gov.hmrc.pillar2.models.financial

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class FinancialItem(
  dueDate:        Option[LocalDate],
  amount:         Option[BigDecimal],
  paymentAmount:  Option[BigDecimal],
  clearingDate:   Option[LocalDate],
  clearingReason: Option[String]
)

object FinancialItem {
  implicit val format: OFormat[FinancialItem] = Json.format[FinancialItem]
}
