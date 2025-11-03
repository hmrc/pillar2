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

package uk.gov.hmrc.pillar2.models.errors

import java.time.ZonedDateTime

sealed trait Pillar2Error extends Exception {
  val message: String
  val code:    String
}

case class MissingHeaderError(headerName: String) extends Pillar2Error {
  val message: String = s"Missing $headerName header"
  val code:    String = "001"
}

case class InvalidJsonError(decodeError: String) extends Pillar2Error {
  val code:    String = "002"
  val message: String = s"Invalid JSON payload: $decodeError"

}

case class ApiInternalServerError(message: String, code: String) extends Pillar2Error

case class ETMPValidationError(code: String, message: String, processingDate: ZonedDateTime) extends Pillar2Error

case object AuthorizationError extends Pillar2Error {
  val message: String = "Not Authorized"
  val code:    String = "401"
}

object ApiInternalServerError {
  val defaultInstance: ApiInternalServerError = ApiInternalServerError(
    message = "Internal server error",
    code = "003"
  )
}
