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

case class UpeRegisterWithoutIdAuditEvent(
  upeRegistration: UpeRegistration
) extends AuditEvent {
  override val auditType:  String  = "CreateP2RegistrationUPENoID"
  override val detailJson: JsValue = Json.toJson(this)
}

object UpeRegisterWithoutIdAuditEvent {
  implicit val formats: Format[UpeRegisterWithoutIdAuditEvent] = Json.format[UpeRegisterWithoutIdAuditEvent]
}

case class FmRegisterWithoutIdAuditEvent(
  nominatedFilingMember: NominatedFilingMember
) extends AuditEvent {
  override val auditType:  String  = "CreateP2RegistrationNFMNoID"
  override val detailJson: JsValue = Json.toJson(this)
}

object FmRegisterWithoutIdAuditEvent {
  implicit val formats: Format[FmRegisterWithoutIdAuditEvent] = Json.format[FmRegisterWithoutIdAuditEvent]
}

case class UpeRegistration(
  registeredinUK:           Boolean,
  entityType:               String,
  ultimateParentEntityName: String,
  addressLine1:             String,
  addressLine2:             String,
  townOrCity:               String,
  region:                   String,
  postCode:                 String,
  country:                  String,
  name:                     String,
  email:                    String,
  telephoneNo:              String
)

object UpeRegistration {
  implicit val formats: Format[UpeRegistration] = Json.format[UpeRegistration]
}

case class NominatedFilingMember(
  registerNomFilingMember:   Boolean,
  registeredinUK:            Boolean,
  nominatedFilingMemberName: String,
  addressLine1:              String,
  addressLine2:              String,
  townOrCity:                String,
  region:                    String,
  postCode:                  String,
  country:                   String,
  name:                      String,
  email:                     String,
  telephoneNo:               String
)

object NominatedFilingMember {
  implicit val formats: Format[NominatedFilingMember] = Json.format[NominatedFilingMember]
}
