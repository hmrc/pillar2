/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.models.obligationsAndSubmissions

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationType
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationStatus
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.Submission

final case class Obligation(
  obligationType: ObligationType,
  status:         ObligationStatus,
  canAmend:       Boolean,
  submissions:    Seq[Submission]
)

object Obligation {
  implicit val reads: Reads[Obligation] =
    (
      (JsPath \ "obligationType").read[ObligationType] and
        (JsPath \ "status").read[ObligationStatus] and
        (JsPath \ "canAmend").read[Boolean] and
        (JsPath \ "submissions").readWithDefault[Seq[Submission]](Seq.empty)
    )(Obligation.apply _)

  implicit val writes: OWrites[Obligation] = Json.writes[Obligation]

  implicit val format: OFormat[Obligation] = OFormat(reads, writes)
}
