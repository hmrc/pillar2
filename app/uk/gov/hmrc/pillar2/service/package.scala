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

import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.errors._
import uk.gov.hmrc.pillar2.models.hip.{ApiFailureResponse, ApiSuccessResponse}

import scala.concurrent.Future

package object service {

  private[service] def convertToApiResult(response: HttpResponse): Future[ApiSuccessResponse] =
    response.status match {
      case 201 =>
        response.json.validate[ApiSuccessResponse] match {
          case JsSuccess(success, _) => Future.successful(success)
          case JsError(error)        => Future.failed(InvalidJsonError(error.toString))
        }
      case 422 =>
        response.json.validate[ApiFailureResponse] match {
          case JsSuccess(apiFailure, _) => Future.failed(ValidationError(apiFailure.errors.code, apiFailure.errors.text))
          case JsError(_)               => Future.failed(InvalidJsonError(response.body))
        }
      case _ =>
        Future.failed(GenericInternalServerError)
    }

}
