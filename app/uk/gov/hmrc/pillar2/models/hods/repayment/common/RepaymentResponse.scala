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

package uk.gov.hmrc.pillar2.models.hods.repayment.common

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class RepaymentResponse(success: RepaymentSuccess)

object RepaymentResponse {
  implicit val format: OFormat[RepaymentResponse] = Json.format[RepaymentResponse]
}

case class RepaymentSuccess(
  processingDate: LocalDate
)

object RepaymentSuccess {
  implicit val format: OFormat[RepaymentSuccess] = Json.format[RepaymentSuccess]
}

final case class Failure(reason: String, code: String)

object Failure {
  implicit val format: OFormat[Failure] = Json.format[Failure]
}
