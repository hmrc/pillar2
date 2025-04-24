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

package uk.gov.hmrc.pillar2.models.orn

import play.api.libs.json.{Json, OFormat}

import java.time.{LocalDate, ZonedDateTime}

case class GetORNSuccess(
  processingDate:       ZonedDateTime,
  accountingPeriodFrom: LocalDate,
  accountingPeriodTo:   LocalDate,
  filedDateGIR:         LocalDate,
  countryGIR:           String,
  reportingEntityName:  String,
  TIN:                  String,
  issuingCountryTIN:    String
)

object GetORNSuccess {
  implicit val format: OFormat[GetORNSuccess] = Json.format[GetORNSuccess]
}
