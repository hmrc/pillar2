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

package uk.gov.hmrc.pillar2.models

import play.api.libs.json._
import play.api.mvc.JavascriptLiteral

sealed trait RowStatus extends Product with Serializable

object RowStatus {
  case object Completed extends RowStatus {
    val value: String = this.toString
  }
  case object InProgress extends RowStatus {
    val value: String = this.toString
  }
  case object NotStarted extends RowStatus {
    val value: String = this.toString
  }

  given format: Format[RowStatus] = new Format[RowStatus] {
    override def reads(json: JsValue): JsResult[RowStatus] =
      json.as[String] match {
        case "Completed"  => JsSuccess[RowStatus](Completed)
        case "InProgress" => JsSuccess[RowStatus](InProgress)
        case "NotStarted" => JsSuccess[RowStatus](NotStarted)
        case other        => JsError(s"Invalid Source System: $other")
      }

    override def writes(sourceSystem: RowStatus): JsValue =
      sourceSystem match {
        case Completed  => JsString("Completed")
        case InProgress => JsString("InProgress")
        case NotStarted => JsString("NotStarted")
      }
  }

  given jsLiteral: JavascriptLiteral[RowStatus] = new JavascriptLiteral[RowStatus] {
    override def to(value: RowStatus): String =
      value match {
        case Completed  => "Completed"
        case InProgress => "InProgress"
        case NotStarted => "NotStarted"
      }
  }

}
