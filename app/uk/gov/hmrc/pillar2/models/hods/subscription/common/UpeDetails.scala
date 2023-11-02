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

package uk.gov.hmrc.pillar2.models.hods.subscription.common

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class UpeDetails(
  safeId:                  Option[String],
  customerIdentification1: Option[String],
  customerIdentification2: Option[String],
  organisationName:        String,
  registrationDate:        LocalDate,
  domesticOnly:            Boolean,
  filingMember:            Boolean
)
/*
domesticOnly: false
                    organisationName: 'International Organisation Inc.'
                    customerIdentification1: '12345678' //Optional
                    customerIdentification2: '12345678' //Optional
                    registrationDate: '2022-01-31'
                    filingMember: false
 */
object UpeDetails {
  implicit val format: OFormat[UpeDetails] = Json.format[UpeDetails]
}

final case class UpeCorrespAddressDetails(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode:     Option[String],
  countryCode:  String
)

object UpeCorrespAddressDetails {
  implicit val format: OFormat[UpeCorrespAddressDetails] = Json.format[UpeCorrespAddressDetails]
}

final case class ContactDetailsType(
  name:         String,
  telephone:    Option[String],
  emailAddress: String
)

object ContactDetailsType {
  implicit val format: OFormat[ContactDetailsType] = Json.format[ContactDetailsType]
}

final case class FilingMemberDetails(
  safeId:                  String,
  customerIdentification1: Option[String],
  customerIdentification2: Option[String],
  organisationName:        String
)

object FilingMemberDetails {
  implicit val format: OFormat[FilingMemberDetails] = Json.format[FilingMemberDetails]
}
