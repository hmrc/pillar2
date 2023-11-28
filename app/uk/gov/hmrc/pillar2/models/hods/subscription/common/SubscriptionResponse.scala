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

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod}

import java.time.LocalDate

object LocalDateImplicits {
  implicit val localDateReads: Reads[LocalDate] = Reads.localDateReads("yyyy-MM-dd")
  implicit val localDateWrites: Writes[LocalDate] = Writes.temporalWrites[LocalDate, java.time.format.DateTimeFormatter](
    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
  )
}

case class SubscriptionResponse(success: SubscriptionSuccess)

object SubscriptionResponse {
  implicit val format: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}

case class SubscriptionSuccess(
  plrReference:             String,
  processingDate:           LocalDate,
  formBundleNumber:         String,
  upeDetails:               UpeDetails,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    PrimaryContactDetails,
  secondaryContactDetails:  SecondaryContactDetails,
  filingMemberDetails:      FilingMemberDetails,
  accountingPeriod:         AccountingPeriod,
  accountStatus:            AccountStatus
)

object SubscriptionSuccess {
  implicit val format: OFormat[SubscriptionSuccess] = Json.format[SubscriptionSuccess]
}

case class AmendSubscriptionSuccess(
  upeDetails:               UpeDetails,
  accountingPeriod:         AccountingPeriod,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    PrimaryContactDetails,
  secondaryContactDetails:  SecondaryContactDetails,
  filingMemberDetails:      FilingMemberDetails
)

object AmendSubscriptionSuccess {
  implicit val format: OFormat[AmendSubscriptionSuccess] = Json.format[AmendSubscriptionSuccess]
}

case class AmendSubscriptionResponse(value: AmendSubscriptionSuccess)

object AmendSubscriptionResponse {
  implicit val format: OFormat[AmendSubscriptionResponse] = Json.format[AmendSubscriptionResponse]
}

case class PrimaryContactDetails(
  name:         String,
  telepphone:   Option[String],
  telephone:    Option[String],
  emailAddress: String
)

object PrimaryContactDetails {
  implicit val format: OFormat[PrimaryContactDetails] = Json.format[PrimaryContactDetails]
}

case class SecondaryContactDetails(
  name:         String,
  telepphone:   Option[String],
  telephone:    Option[String],
  emailAddress: String
)

object SecondaryContactDetails {
  implicit val format: OFormat[SecondaryContactDetails] = Json.format[SecondaryContactDetails]
}

case class DashboardInfo(
  organisationName: String,
  registrationDate: LocalDate
)

object DashboardInfo {
  implicit val format: OFormat[DashboardInfo] = Json.format[DashboardInfo]
}
