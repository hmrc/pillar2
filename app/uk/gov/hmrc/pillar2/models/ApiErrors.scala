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

package uk.gov.hmrc.pillar2.models

import play.api.libs.json.Json

trait ApiErrors extends Throwable

case object JsResultError extends ApiErrors
case object UnexpectedResponse extends ApiErrors

final case class FinancialDataError(code: String, reason: String) extends ApiErrors

final case class FinancialDataErrorResponses(failures: Seq[FinancialDataError]) extends ApiErrors

object FinancialDataError {
  implicit val formatException = Json.format[FinancialDataError]
}

object FinancialDataErrorResponses {
  implicit val format = Json.format[FinancialDataErrorResponses]
}
