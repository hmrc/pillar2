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
import uk.gov.hmrc.pillar2.models.grs.{BusinessVerificationResult, GrsRegistrationResult}

import java.time.LocalDate

final case class IncorporatedEntityRegistrationData(
  companyProfile:       CompanyProfile,
  ctutr:                String,
  identifiersMatch:     Boolean,
  businessVerification: Option[BusinessVerificationResult],
  registration:         GrsRegistrationResult
)

object IncorporatedEntityRegistrationData {
  implicit val format: OFormat[IncorporatedEntityRegistrationData] =
    Json.format[IncorporatedEntityRegistrationData]
}

final case class CompanyProfile(
  companyName:            String,
  companyNumber:          String,
  dateOfIncorporation:    Option[LocalDate],
  unsanitisedCHROAddress: IncorporatedEntityAddress
)

object CompanyProfile {
  implicit val format: OFormat[CompanyProfile] =
    Json.format[CompanyProfile]
}

final case class IncorporatedEntityAddress(
  address_line_1: Option[String],
  address_line_2: Option[String],
  country:        Option[String],
  locality:       Option[String],
  po_box:         Option[String],
  postal_code:    Option[String],
  premises:       Option[String],
  region:         Option[String]
)

object IncorporatedEntityAddress {
  implicit val format: OFormat[IncorporatedEntityAddress] =
    Json.format[IncorporatedEntityAddress]
}
