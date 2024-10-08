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

package uk.gov.hmrc.pillar2.models.obligation

import play.api.libs.json._

sealed trait ObligationStatus
object ObligationStatus {
  case object Open extends ObligationStatus
  case object Fulfilled extends ObligationStatus

  val values: Seq[ObligationStatus] = Seq(
    Open,
    Fulfilled
  )

  implicit val format: Format[ObligationStatus] = new Format[ObligationStatus] {
    override def reads(json: JsValue): JsResult[ObligationStatus] =
      json.as[String] match {
        case "open"      => JsSuccess(Open)
        case "fulfilled" => JsSuccess(Fulfilled)
        case _           => JsError("Invalid obligation status")
      }

    override def writes(ObligationStatus: ObligationStatus): JsValue =
      ObligationStatus match {
        case Open      => JsString("open")
        case Fulfilled => JsString("fulfilled")
      }
  }

}
