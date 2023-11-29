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

package uk.gov.hmrc.pillar2.models

import play.api.libs.json._
import uk.gov.hmrc.pillar2.models.hods.subscription.common.FilingMemberDetails
import uk.gov.hmrc.pillar2.models.registration.RegistrationInfo

case class SubscriptionData(
  upeRegisteredInUK:        Boolean,
  upeNameRegistration:      String,
  upeRegInformation:        RegistrationInfo,
  upeRegisteredAddress:     UKAddress,
  subPrimaryContactName:    String,
  subPrimaryEmail:          String,
  subSecondaryContactName:  String,
  subSecondaryCapturePhone: String,
  subSecondaryEmail:        String,
  FmSafeID:                 String,
  subFilingMemberDetails:   FilingMemberDetails,
  subAccountingPeriod:      AccountingPeriod,
  subAccountStatus:         AccountStatus
)

object SubscriptionData {
  implicit val format: OFormat[SubscriptionData] = Json.format[SubscriptionData]
}
