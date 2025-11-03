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

package uk.gov.hmrc.pillar2.handlers

import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.pillar2.models.errors._

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class Pillar2ErrorHandler extends HttpErrorHandler with Logging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(
      Status(statusCode)(Json.toJson(Pillar2ApiError(statusCode.toString, message, processingDate = None)))
    )

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case e: Pillar2Error =>
        val ret = e match {
          case error: MissingHeaderError =>
            BadRequest(
              Json.toJson(
                Pillar2ApiError(error.code, error.message, processingDate = None)
              )
            )
          case error: ETMPValidationError =>
            UnprocessableEntity(
              Json.toJson(
                Pillar2ApiError(error.code, error.message, Some(error.processingDate))
              )
            )
          case error: InvalidJsonError => InternalServerError(Json.toJson(Pillar2ApiError(error.code, error.message, processingDate = None)))
          case error: ApiInternalServerError =>
            InternalServerError(
              Json.toJson(
                Pillar2ApiError(error.code, error.message, processingDate = None)
              )
            )
          case error @ AuthorizationError =>
            Unauthorized(
              Json.toJson(
                Pillar2ApiError(error.code, error.message, processingDate = None)
              )
            )
        }
        logger.warn(s"Caught Pillar2Error. Returning ${ret.header.status} statuscode", exception)
        Future.successful(ret)
      case _ =>
        logger.warn(s"Caught unhandled exception. Returning InternalServerError with default values.", exception)
        Future.successful(
          InternalServerError(
            Json.toJson(
              Pillar2ApiError(ApiInternalServerError.defaultInstance.code, ApiInternalServerError.defaultInstance.message, processingDate = None)
            )
          )
        )
    }
}
