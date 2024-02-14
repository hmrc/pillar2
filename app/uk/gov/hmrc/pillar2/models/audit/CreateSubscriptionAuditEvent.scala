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

import play.api.libs.json.{Format, JsValue, Json, OFormat}
import uk.gov.hmrc.pillar2.models.AccountingPeriod
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{ContactDetailsType, FilingMemberDetails, UpeCorrespAddressDetails, UpeDetails}

import java.time.{LocalDate, LocalDateTime}

case class CreateSubscriptionAuditEvent(
  upeDetails:               UpeDetails,
  accountingPeriod:         AccountingPeriod,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    ContactDetailsType,
  secondaryContactDetails:  Option[ContactDetailsType],
  filingMemberDetails:      Option[FilingMemberDetails],
  plrReference:             String,
  processingDate:           String
) extends AuditEvent {
  override val auditType:  String  = "createPillar2Subscription"
  override val detailJson: JsValue = Json.toJson(this)
}

object CreateSubscriptionAuditEvent {
  implicit val formats: Format[CreateSubscriptionAuditEvent] = Json.format[CreateSubscriptionAuditEvent]
}

case class SubscriptionSuccessResponse(plrReference: String, formBundleNumber: String, processingDate: LocalDateTime)

object SubscriptionSuccessResponse {
  implicit val format: OFormat[SubscriptionSuccessResponse] = Json.format[SubscriptionSuccessResponse]
}

case class SuccessResponse(success: SubscriptionSuccessResponse)

object SuccessResponse {
  implicit val format: OFormat[SuccessResponse] = Json.format[SuccessResponse]
}
