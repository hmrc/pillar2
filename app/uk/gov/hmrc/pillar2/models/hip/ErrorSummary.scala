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

package uk.gov.hmrc.pillar2.models.hip

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError

case class ErrorSummary(code: String, message: String)

object ErrorSummary {
  val result_500: Result = InternalServerError(Json.toJson(ErrorSummary("500", "Internal server error")))

  implicit val format: OFormat[ErrorSummary] = Json.format[ErrorSummary]
}
