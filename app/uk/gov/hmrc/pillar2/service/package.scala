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

package uk.gov.hmrc.pillar2

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Reads}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.btn.BTNSuccessResponse
import uk.gov.hmrc.pillar2.models.errors._
import uk.gov.hmrc.pillar2.models.hip.{ApiSuccessResponse, ErrorFailureResponse, UnprocessableFailureResponse}
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationsAndSubmissionsResponse
import uk.gov.hmrc.pillar2.models.orn.{GetORNSuccessResponse, ORNSuccessResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

package object service extends Logging {
  private[service] def convertToResult[A](response: HttpResponse)(implicit reads: Reads[A]): Future[A] = {
    logger.info(s"Converting to API result with status ${response.status}")
    response.status match {
      case 200 | 201 =>
        // we're using try because HttpResponse.json is not a pure function and can throw an exception
        Try(response.json.validate[A]) match {
          case Success(JsSuccess(success, _)) => Future.successful(success)
          case Success(JsError(error))        => Future.failed(InvalidJsonError(error.toString))
          case Failure(exception)             => Future.failed(InvalidJsonError(exception.getMessage))
        }
      case 422 =>
        Try(response.json.validate[UnprocessableFailureResponse]) match {
          case Success(JsSuccess(apiFailure, _)) =>
            Future.failed(ETMPValidationError(apiFailure.errors.code, apiFailure.errors.text, apiFailure.errors.processingDate))
          case Success(JsError(_)) => Future.failed(InvalidJsonError(response.body))
          case Failure(exception)  => Future.failed(InvalidJsonError(exception.getMessage))
        }
      case 400 | 500 =>
        logger.error(s"Received ${response.status} status from downstream. Attempting to parse code & message.")
        Try(response.json.validate[ErrorFailureResponse]) match {
          case Success(JsSuccess(apiFailure, _)) =>
            Future.failed {
              logger.info(s"Parsed message & code from ${response.status} response. Surfacing to client.")
              ApiInternalServerError(
                message = apiFailure.error.message,
                code = apiFailure.error.code
              )
            }
          case _ =>
            Future.failed {
              logger.error(s"Unable to parse message & code from ${response.status} response. Returning default values to client.")
              ApiInternalServerError.defaultInstance
            }
        }
      case status =>
        logger.error(s"Received invalid status from downstream: $status. Returning default values to client.")
        Future.failed(ApiInternalServerError.defaultInstance)
    }
  }

  private[service] def convertToUKTRApiResult(response: HttpResponse): Future[ApiSuccessResponse] =
    convertToResult[ApiSuccessResponse](response)

  private[service] def convertToBTNApiResult(response: HttpResponse): Future[BTNSuccessResponse] =
    convertToResult[BTNSuccessResponse](response)

  private[service] def convertToObligationsAndSubmissionsApiResult(response: HttpResponse): Future[ObligationsAndSubmissionsResponse] =
    convertToResult[ObligationsAndSubmissionsResponse](response)

  private[service] def convertToORNApiResult(response: HttpResponse): Future[ORNSuccessResponse] =
    convertToResult[ORNSuccessResponse](response)

  private[service] def convertToGetORNApiResult(response: HttpResponse): Future[GetORNSuccessResponse] =
    convertToResult[GetORNSuccessResponse](response)
}
