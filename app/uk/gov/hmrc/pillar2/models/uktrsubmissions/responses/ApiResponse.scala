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

package uk.gov.hmrc.pillar2.models.uktrsubmissions.responses

import play.api.libs.json._

sealed trait ApiResponse

case class SuccessResponse(
  processingDate:   String,
  formBundleNumber: String,
  chargeReference:  Option[String]
) extends ApiResponse

sealed trait ErrorResponse extends ApiResponse

case class SingleError(
  code:    String,
  message: String,
  logId:   String
) extends ErrorResponse

case class ValidationErrors(
  processingDate: String,
  code:           String,
  text:           String
) extends ErrorResponse

case class EmptyErrorResponse(statusCode: Int) extends ErrorResponse

object ApiResponse {
  implicit val successResponseFormat:    Format[SuccessResponse]    = Json.format[SuccessResponse]
  implicit val singleErrorFormat:        Format[SingleError]        = Json.format[SingleError]
  implicit val validationErrorsFormat:   Format[ValidationErrors]   = Json.format[ValidationErrors]
  implicit val emptyErrorResponseFormat: Format[EmptyErrorResponse] = Json.format[EmptyErrorResponse]

  implicit val apiResponseWrites: Writes[ApiResponse] = Writes {
    case success: SuccessResponse  => Json.toJson(success)(successResponseFormat)
    case error:   SingleError      => Json.toJson(error)(singleErrorFormat)
    case errors:  ValidationErrors => Json.toJson(errors)(validationErrorsFormat)
    case EmptyErrorResponse(_) => Json.obj()
  }
}
