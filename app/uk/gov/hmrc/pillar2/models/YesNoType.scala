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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait YesNoType extends Product with Serializable

object YesNoType {

  case object Yes extends YesNoType

  case object No extends YesNoType

  //to handle contravariant to invariant in 2.8 play-json
  val yes: YesNoType = Yes
  val no:  YesNoType = No

  implicit val format: Format[YesNoType] = new Format[YesNoType] {
    override def reads(json: JsValue): JsResult[YesNoType] =
      json.as[String] match {
        case "yes" => JsSuccess(Yes)
        case "no"  => JsSuccess(No)
        case _     => JsError("Invalid movement type")
      }

    override def writes(yesNoType: YesNoType): JsValue =
      yesNoType match {
        case Yes => JsString("yes")
        case No  => JsString("no")
      }
  }

}
