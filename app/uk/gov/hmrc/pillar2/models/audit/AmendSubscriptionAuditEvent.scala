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

package uk.gov.hmrc.pillar2.models.audit

import play.api.libs.json.{Format, JsValue, Json}
import uk.gov.hmrc.pillar2.models.AccountingPeriodAmend
import uk.gov.hmrc.pillar2.models.hods.subscription.common._

case class AmendSubscriptionSuccessAuditEvent(
  replaceFilingMember:      Boolean,
  upeDetails:               UpeDetailsAmend,
  accountingPeriod:         AccountingPeriodAmend,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    ContactDetailsType,
  secondaryContactDetails:  Option[ContactDetailsType],
  filingMemberDetails:      Option[FilingMemberAmendDetails],
  processingDate:           String
) extends AuditEvent {
  override val auditType:  String  = "updatePillar2Subscription"
  override val detailJson: JsValue = Json.toJson(this)
}

object AmendSubscriptionSuccessAuditEvent {
  implicit val formats: Format[AmendSubscriptionSuccessAuditEvent] = Json.format[AmendSubscriptionSuccessAuditEvent]
}
