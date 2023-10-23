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

package uk.gov.hmrc.pillar2.models.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, RowStatus}

case class Subscription(
  domesticOrMne:             MneOrDomestic,
  groupDetailStatus:         RowStatus,
  accountingPeriod:          Option[AccountingPeriod] = None,
  primaryContactName:        Option[String] = None,
  primaryContactEmail:       Option[String] = None,
  primaryContactTelephone:   Option[String] = None,
  secondaryContactName:      Option[String] = None,
  secondaryContactEmail:     Option[String] = None,
  secondaryContactTelephone: Option[String] = None,
  correspondenceAddress:     Option[SubscriptionAddress] = None,
  accountStatus:             Option[AccountStatus] = None
)

object Subscription {
  implicit val format: OFormat[Subscription] = Json.format[Subscription]
}
