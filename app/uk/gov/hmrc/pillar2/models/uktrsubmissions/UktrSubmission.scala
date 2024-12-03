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

package uk.gov.hmrc.pillar2.models.uktrsubmissions

import play.api.libs.json._

import java.time.LocalDate

trait UktrSubmission {
  val accountingPeriodFrom: LocalDate
  val accountingPeriodTo:   LocalDate
  val obligationMTT:        Boolean
  val electionUKGAAP:       Boolean
  val liabilities:          Liability
}

object UktrSubmission {
  implicit val uktrSubmissionReads: Reads[UktrSubmission] = (json: JsValue) =>
    if ((json \ "liabilities" \ "returnType").isEmpty) {
      json.validate[UktrSubmissionData]
    } else {
      json.validate[UktrSubmissionNilReturn]
    }

  implicit val uktrSubmissionWrites: Writes[UktrSubmission] = new Writes[UktrSubmission] {
    def writes(submission: UktrSubmission): JsValue = submission match {
      case data:      UktrSubmissionData      => Json.toJson(data)(Json.writes[UktrSubmissionData])
      case nilReturn: UktrSubmissionNilReturn => Json.toJson(nilReturn)(Json.writes[UktrSubmissionNilReturn])
      case _ => throw new IllegalArgumentException("Unknown UktrSubmission type")
    }
  }
}
