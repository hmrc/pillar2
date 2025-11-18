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

package uk.gov.hmrc.pillar2

import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.hods.ErrorDetails

import scala.util.{Success, Try}

package object controllers {
  extension (error: JsError) {
    def toLogFormat: String = Json.prettyPrint(JsError.toJson(error))
  }

  def convertToResult(
    httpResponse: HttpResponse
  )(using logger: Logger): Result =
    httpResponse.status match {
      case CREATED   => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)
      case BAD_REQUEST =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        BadRequest(httpResponse.body)

      case FORBIDDEN =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        Forbidden(httpResponse.body)

      case SERVICE_UNAVAILABLE =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        ServiceUnavailable(httpResponse.body)

      case UNPROCESSABLE_ENTITY =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        UnprocessableEntity(httpResponse.body)

      case INTERNAL_SERVER_ERROR =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        InternalServerError(httpResponse.body)

      case CONFLICT =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        Conflict(httpResponse.body)

      case OK =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        Ok(httpResponse.body)

      case _ =>
        logger.info(s"convertToResult - Received Response body - ${httpResponse.body}")
        logDownStreamError(httpResponse.body)
        InternalServerError(httpResponse.body)

    }

  private def logDownStreamError(
    body:         String
  )(using logger: Logger): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetails])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.warn(
          s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}"
        )
      case _ =>
        logger.warn("Error with submission but return is not a valid json")
    }
  }
}
