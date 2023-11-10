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

import play.api.libs.json.{JsNull, JsValue, Json, OFormat, Writes}

final case class AccountStatus(
  inactive: Boolean
)

object AccountStatus {
  implicit val format: OFormat[AccountStatus] = Json.format[AccountStatus]

  implicit val optionWrites: Writes[Option[AccountStatus]] = new Writes[Option[AccountStatus]] {
    def writes(option: Option[AccountStatus]): JsValue = option match {
      case Some(accountStatus) => Json.toJson(accountStatus)(format)
      case None                => JsNull
    }
  }
  implicit val writes: Writes[AccountStatus] = Json.writes[AccountStatus]

  type AccountStatusOpt = Option[AccountStatus]
  implicit val accountStatusOptWrites: Writes[AccountStatusOpt] = optionWrites

}
