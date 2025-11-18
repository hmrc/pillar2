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

package uk.gov.hmrc.pillar2.models.grs

import play.api.libs.json._

sealed trait EntityType extends Product with Serializable

object EntityType {

  case object UKLimitedCompany extends EntityType
  case object LimitedLiabilityPartnership extends EntityType

  given format: Format[EntityType] = new Format[EntityType] {
    override def reads(json: JsValue): JsResult[EntityType] =
      json.as[String] match {
        case "ukLimitedCompany"            => JsSuccess(UKLimitedCompany)
        case "limitedLiabilityPartnership" => JsSuccess(LimitedLiabilityPartnership)
        case _                             => JsError("Invalid movement type")
      }

    override def writes(entityType: EntityType): JsValue =
      entityType match {
        case UKLimitedCompany            => JsString("ukLimitedCompany")
        case LimitedLiabilityPartnership => JsString("limitedLiabilityPartnership")
      }
  }
}
