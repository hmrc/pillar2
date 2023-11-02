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

package uk.gov.hmrc.pillar2.models.hods.subscription

import play.api.libs.json._

case class Identifier(key: String, value: String)
object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}

case class Enrolment(
  service:               String,
  state:                 String,
  friendlyName:          String,
  enrolmentDate:         String, // Ideally, this should be a more precise type like java.time.Instant
  failedActivationCount: Int,
  activationDate:        String, // Again, consider java.time.Instant or another date-time type
  identifiers:           List[Identifier]
)
object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class EnrolmentResponse(startRecord: Int, totalRecords: Int, enrolments: List[Enrolment])
object EnrolmentResponse {
  implicit val format: Format[EnrolmentResponse] = Json.format[EnrolmentResponse]
}
