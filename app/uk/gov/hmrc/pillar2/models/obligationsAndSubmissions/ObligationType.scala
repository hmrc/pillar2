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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait ObligationType
object ObligationType {
  case object Pillar2TaxReturn extends ObligationType
  case object GlobeInformationReturn extends ObligationType

  val values: Seq[ObligationType] = Seq(
    Pillar2TaxReturn,
    GlobeInformationReturn
  )

  implicit val format: Format[ObligationType] = new Format[ObligationType] {
    override def reads(json: JsValue): JsResult[ObligationType] =
      json.as[String] match {
        case "Pillar2TaxReturn"       => JsSuccess(Pillar2TaxReturn)
        case "GlobeInformationReturn" => JsSuccess(GlobeInformationReturn)
        case _                        => JsError("Invalid obligation type")
      }

    override def writes(ObligationType: ObligationType): JsValue =
      ObligationType match {
        case Pillar2TaxReturn       => JsString("Pillar2TaxReturn")
        case GlobeInformationReturn => JsString("GlobeInformationReturn")
      }
  }
}
