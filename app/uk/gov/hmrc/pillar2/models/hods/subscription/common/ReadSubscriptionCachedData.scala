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
import uk.gov.hmrc.pillar2.models.subscription.MneOrDomestic
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, NonUKAddress}

case class ReadSubscriptionCachedData(
  plrReference:                Option[String],
  subMneOrDomestic:            MneOrDomestic,
  subAccountingPeriod:         AccountingPeriod,
  subPrimaryContactName:       String,
  subPrimaryEmail:             String,
  subPrimaryPhonePreference:   Boolean,
  subPrimaryCapturePhone:      Option[String],
  subAddSecondaryContact:      Boolean,
  subSecondaryContactName:     Option[String],
  subSecondaryEmail:           Option[String],
  subSecondaryCapturePhone:    Option[String],
  subSecondaryPhonePreference: Option[Boolean],
  subRegisteredAddress:        NonUKAddress,
  accountStatus:               Option[AccountStatus],
  organisationName:            Option[String]
)

object ReadSubscriptionCachedData {
  given format: OFormat[ReadSubscriptionCachedData] = Json.format[ReadSubscriptionCachedData]
}
