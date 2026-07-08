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
import play.api.libs.json.{JsError, Json}
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.hods.ErrorDetails

import scala.util.Try

package object controllers {

  extension (error: JsError) {
    def toLogFormat: String = Json.prettyPrint(JsError.toJson(error))
  }

  def convertToResult(httpResponse: HttpResponse)(using logger: Logger): Result =
    httpResponse.status match {
      case OK        => Ok(httpResponse.body)
      case CREATED   => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)
      case BAD_REQUEST =>
        logDownstreamError(httpResponse.body)
        BadRequest(httpResponse.body)
      case FORBIDDEN =>
        logDownstreamError(httpResponse.body)
        Forbidden(httpResponse.body)
      case SERVICE_UNAVAILABLE =>
        logDownstreamError(httpResponse.body)
        ServiceUnavailable(httpResponse.body)
      case UNPROCESSABLE_ENTITY =>
        logDownstreamError(httpResponse.body)
        UnprocessableEntity(httpResponse.body)
      case INTERNAL_SERVER_ERROR =>
        logDownstreamError(httpResponse.body)
        InternalServerError(httpResponse.body)
      case CONFLICT =>
        logDownstreamError(httpResponse.body)
        Conflict(httpResponse.body)
      case _ =>
        logDownstreamError(httpResponse.body)
        InternalServerError(httpResponse.body)
    }

  private def logDownstreamError(body: String)(using logger: Logger): Unit = {
    val errorMessage: Option[String] =
      Try(Json.parse(body)).toOption.flatMap { json =>
        json
          .asOpt[ErrorDetails]
          .map { errorDetails =>
            errorDetails.errorDetail.sourceFaultDetail
              .map(_.detail.mkString("; "))
              .getOrElse(s"${errorDetails.errorDetail.errorCode} - ${errorDetails.errorDetail.errorMessage}")
          }
      }

    errorMessage match {
      case Some(msg) => logger.warn(s"Error with submission: $msg")
      case None      => logger.warn(s"Error with submission - unrecognised json body")
    }
  }

}
