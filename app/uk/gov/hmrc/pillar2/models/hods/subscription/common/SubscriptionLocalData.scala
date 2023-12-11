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
import uk.gov.hmrc.pillar2.models.subscription.{ExtraSubscription, MneOrDomestic}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, NonUKAddress}

case class SubscriptionLocalData(
  plrReference:                String,
  subMneOrDomestic:            MneOrDomestic,
  upeNameRegistration:         String,
  subPrimaryContactName:       String,
  subPrimaryEmail:             String,
  subPrimaryCapturePhone:      Option[String],
  subPrimaryPhonePreference:   Boolean,
  subSecondaryContactName:     Option[String],
  subAddSecondaryContact:      Boolean,
  subSecondaryEmail:           Option[String],
  subSecondaryCapturePhone:    Option[String],
  subSecondaryPhonePreference: Boolean,
  subRegisteredAddress:        NonUKAddress,
  subFilingMemberDetails:      Option[FilingMemberDetails],
  subAccountingPeriod:         AccountingPeriod,
  subAccountStatus:            Option[AccountStatus],
  NominateFilingMember:        Boolean,
  subExtraSubscription:        ExtraSubscription,
  subRegistrationDate:         LocalDate,
  fmDashboard:                 DashboardInfo
)

object SubscriptionLocalData {
  implicit val format: OFormat[SubscriptionLocalData] = Json.format[SubscriptionLocalData]
}
