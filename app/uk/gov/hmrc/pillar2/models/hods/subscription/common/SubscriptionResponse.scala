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
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, AccountingPeriodAmend}

import java.time.LocalDate

case class SubscriptionResponse(success: SubscriptionSuccess)

object SubscriptionResponse {
  given format: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}

case class SubscriptionSuccess(
  formBundleNumber:         String,
  upeDetails:               UpeDetails,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    ContactDetailsType,
  secondaryContactDetails:  Option[ContactDetailsType],
  filingMemberDetails:      Option[FilingMemberDetails],
  accountingPeriod:         AccountingPeriod,
  accountStatus:            Option[AccountStatus]
)

object SubscriptionSuccess {
  given format: OFormat[SubscriptionSuccess] = Json.format[SubscriptionSuccess]
}

final case class AmendSubscriptionSuccess(
  replaceFilingMember:      Boolean,
  upeDetails:               UpeDetailsAmend,
  accountingPeriod:         AccountingPeriodAmend,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    ContactDetailsType,
  secondaryContactDetails:  Option[ContactDetailsType],
  filingMemberDetails:      Option[FilingMemberAmendDetails]
)

object AmendSubscriptionSuccess {
  given format: OFormat[AmendSubscriptionSuccess] = Json.format[AmendSubscriptionSuccess]
}

final case class AmendSubscriptionInput(value: AmendSubscriptionSuccess)

object AmendSubscriptionInput {
  given format: OFormat[AmendSubscriptionInput] = Json.format[AmendSubscriptionInput]
}

final case class AmendResponse(success: AmendSubscriptionSuccessResponse)

object AmendResponse {
  given format: OFormat[AmendResponse] = Json.format[AmendResponse]
}

final case class AmendSubscriptionSuccessResponse(processingDate: String, formBundleNumber: String)

object AmendSubscriptionSuccessResponse {
  given format: OFormat[AmendSubscriptionSuccessResponse] = Json.format[AmendSubscriptionSuccessResponse]
}

final case class AmendSubscriptionFailureResponse(failures: Array[Failure])

object AmendSubscriptionFailureResponse {
  given format: OFormat[AmendSubscriptionFailureResponse] = Json.format[AmendSubscriptionFailureResponse]
}

final case class Failure(reason: String, code: String)

object Failure {
  given format: OFormat[Failure] = Json.format[Failure]
}

case class DashboardInfo(
  organisationName: String,
  registrationDate: LocalDate
)

object DashboardInfo {
  given format: OFormat[DashboardInfo] = Json.format[DashboardInfo]
}
