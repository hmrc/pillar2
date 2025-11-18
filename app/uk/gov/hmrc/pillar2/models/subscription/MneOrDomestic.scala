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

package uk.gov.hmrc.pillar2.models.subscription

import play.api.libs.json._

sealed trait MneOrDomestic extends Product with Serializable

object MneOrDomestic {

  case object UkAndOther extends MneOrDomestic

  case object Uk extends MneOrDomestic

  //to handle contravariant to invariant in 2.8 play-json
  val ukAndOther: MneOrDomestic = UkAndOther
  val uk:         MneOrDomestic = Uk

  given format: Format[MneOrDomestic] = new Format[MneOrDomestic] {
    override def reads(json: JsValue): JsResult[MneOrDomestic] =
      json.as[String] match {
        case "ukAndOther" => JsSuccess(UkAndOther)
        case "uk"         => JsSuccess(Uk)
        case _            => JsError("Invalid movement type")
      }

    override def writes(yesNoType: MneOrDomestic): JsValue =
      yesNoType match {
        case UkAndOther => JsString("ukAndOther")
        case Uk         => JsString("uk")
      }
  }

}
