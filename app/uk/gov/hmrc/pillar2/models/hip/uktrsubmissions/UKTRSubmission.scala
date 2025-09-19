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

package uk.gov.hmrc.pillar2.models.hip.uktrsubmissions

import play.api.libs.json._

import java.time.LocalDate

trait UKTRSubmission {
  val accountingPeriodFrom: LocalDate
  val accountingPeriodTo:   LocalDate
  val obligationMTT:        Boolean
  val electionUKGAAP:       Option[Boolean]
  val liabilities:          Liability
}

object UKTRSubmission {
  implicit val uktrSubmissionReads: Reads[UKTRSubmission] = (json: JsValue) =>
    if ((json \ "liabilities" \ "returnType").isEmpty) {
      json.validate[UKTRSubmissionData]
    } else {
      json.validate[UKTRSubmissionNilReturn]
    }

  implicit val uktrSubmissionWrites: Writes[UKTRSubmission] = new Writes[UKTRSubmission] {
    def writes(submission: UKTRSubmission): JsValue = submission match {
      case data:      UKTRSubmissionData      => Json.toJson(data)(Json.writes[UKTRSubmissionData])
      case nilReturn: UKTRSubmissionNilReturn => Json.toJson(nilReturn)(Json.writes[UKTRSubmissionNilReturn])
      case _ => throw new IllegalArgumentException("Unknown UKTRSubmission type")
    }
  }
}
