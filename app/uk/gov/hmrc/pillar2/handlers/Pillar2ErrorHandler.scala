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

import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.pillar2.models.errors._

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.http.HttpErrorHandler


@Singleton
class Pillar2ErrorHandler extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = ???

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
        case e : Pillar2Error => e match {
          case error : MissingHeaderError => Future.successful(BadRequest(Json.toJson(Pillar2ApiError(error.code, error.message))))
          case error : ValidationError => Future.successful(UnprocessableEntity(Json.toJson(Pillar2ApiError(error.code, error.message))))
          case error : InvalidJsonError => Future.successful(BadRequest(Json.toJson(Pillar2ApiError(error.code, error.message))))
          case error @ GenericInternalServerError => Future.successful(InternalServerError(Json.toJson(Pillar2ApiError(error.code, error.message))))
        }
      case _ =>
        Future.successful(InternalServerError(Json.toJson(Pillar2ApiError("006", "An unexpected error occurred"))))
    }
  }
} 