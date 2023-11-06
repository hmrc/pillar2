/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pillar2.models.grs.EntityType

import java.time.LocalDate

case class Registration(
  isUPERegisteredInUK: Boolean,
  orgType:             Option[EntityType] = None,
  withIdRegData:       Option[GrsResponse] = None,
  withoutIdRegData:    Option[WithoutIdRegData] = None,
  registrationInfo:    Option[RegistrationInfo] = None
)

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]
}

case class RegistrationInfo(
  crn:              String,
  utr:              String,
  safeId:           String,
  registrationDate: LocalDate,
  filingMember:     Boolean
)

object RegistrationInfo {
  implicit val format: OFormat[RegistrationInfo] = Json.format[RegistrationInfo]
}
