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

import play.api.libs.json.{JsValue, Json, OFormat, OWrites}
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{AmendSubscriptionSuccess, SubscriptionResponse}

case class AmendSubscriptionSuccessAuditEvent(
  requestData:  AmendSubscriptionSuccess,
  responseData: AuditResponseReceived
) extends AuditEvent {
  override val auditType:  String  = "UpdateP2Subscription"
  override val detailJson: JsValue = Json.toJson(this)
}

object AmendSubscriptionSuccessAuditEvent {
  implicit val format: OFormat[AmendSubscriptionSuccessAuditEvent] = Json.format[AmendSubscriptionSuccessAuditEvent]
  implicit val writes: OWrites[AmendSubscriptionSuccessAuditEvent] = Json.writes[AmendSubscriptionSuccessAuditEvent]
}

case class AmendSubscriptionFailedAuditEvent(
  requestData:  AmendSubscriptionSuccess,
  responseData: AuditResponseReceived
) extends AuditEvent {
  override val auditType:  String  = "UpdateP2Subscription"
  override val detailJson: JsValue = Json.toJson(this)
}

object AmendSubscriptionFailedAuditEvent {
  implicit val format: OFormat[AmendSubscriptionFailedAuditEvent] = Json.format[AmendSubscriptionFailedAuditEvent]
  implicit val writes: OWrites[AmendSubscriptionFailedAuditEvent] = Json.writes[AmendSubscriptionFailedAuditEvent]
}
