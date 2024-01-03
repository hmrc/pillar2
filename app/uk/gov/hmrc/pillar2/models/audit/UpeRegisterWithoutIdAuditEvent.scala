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
import uk.gov.hmrc.pillar2.models.hods.RegisterWithoutIDRequest

case class UpeRegisterWithoutIdAuditEvent(
  requestData:  RegisterWithoutIDRequest,
  responseData: AuditResponseReceived
) extends AuditEvent {
  override val auditType:  String  = "CreateP2RegistrationUPENoID"
  override val detailJson: JsValue = Json.toJson(this)
}

object UpeRegisterWithoutIdAuditEvent {
  implicit val format: OFormat[UpeRegisterWithoutIdAuditEvent] = Json.format[UpeRegisterWithoutIdAuditEvent]
  implicit val writes: OWrites[UpeRegisterWithoutIdAuditEvent] = Json.writes[UpeRegisterWithoutIdAuditEvent]
}

case class FmRegisterWithoutIdAuditEvent(
  requestData:  RegisterWithoutIDRequest,
  responseData: AuditResponseReceived
) extends AuditEvent {
  override val auditType:  String  = "CreateP2RegistrationNFMNoID"
  override val detailJson: JsValue = Json.toJson(this)
}

object FmRegisterWithoutIdAuditEvent {
  implicit val format: OFormat[FmRegisterWithoutIdAuditEvent] = Json.format[FmRegisterWithoutIdAuditEvent]
  implicit val writes: OWrites[FmRegisterWithoutIdAuditEvent] = Json.writes[FmRegisterWithoutIdAuditEvent]
}
