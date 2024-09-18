/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.models.hods

import play.api.libs.json.{Json, OFormat}

final case class SourceFaultDetail(detail: Seq[String]) // Make case class final

object SourceFaultDetail {
  implicit val format: OFormat[SourceFaultDetail] = Json.format[SourceFaultDetail] // Explicit type added
}

final case class ErrorDetail( // Make case class final
  timestamp:         String,
  correlationId:     Option[String],
  errorCode:         String,
  errorMessage:      String,
  source:            String,
  sourceFaultDetail: Option[SourceFaultDetail]
)

object ErrorDetail {
  implicit val format: OFormat[ErrorDetail] = Json.format[ErrorDetail] // Explicit type added
}

final case class ErrorDetails(errorDetail: ErrorDetail) // Make case class final

object ErrorDetails {
  implicit val format: OFormat[ErrorDetails] = Json.format[ErrorDetails] // Explicit type added
}
