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

final case class AuditResponseReceived(status: Int, responseData: JsValue)

object AuditResponseReceived {
  implicit val format: OFormat[AuditResponseReceived] = Json.format[AuditResponseReceived]
  implicit val writes: OWrites[AuditResponseReceived] = Json.writes[AuditResponseReceived]
}
