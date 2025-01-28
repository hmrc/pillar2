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

sealed trait SubmissionType
object SubmissionType {
  case object BTN extends SubmissionType
  case object GIR extends SubmissionType
  case object UKTR extends SubmissionType

  val values: Seq[SubmissionType] = Seq(
    BTN,
    GIR,
    UKTR
  )

  implicit val format: Format[SubmissionType] = new Format[SubmissionType] {
    override def reads(json: JsValue): JsResult[SubmissionType] =
      json.as[String] match {
        case "BTN"  => JsSuccess(BTN)
        case "GIR"  => JsSuccess(GIR)
        case "UKTR" => JsSuccess(UKTR)
        case _      => JsError("Invalid submission type")
      }

    override def writes(SubmissionType: SubmissionType): JsValue =
      SubmissionType match {
        case BTN  => JsString("BTN")
        case GIR  => JsString("GIR")
        case UKTR => JsString("UKTR")
      }
  }
}
