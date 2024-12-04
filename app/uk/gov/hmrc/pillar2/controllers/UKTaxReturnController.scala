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

package uk.gov.hmrc.pillar2.controllers

import play.api.mvc._
import play.api.libs.json._
import uk.gov.hmrc.pillar2.service.UKTaxReturnService
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.models.uktrsubmissions.UktrSubmission
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.pillar2.models.SimpleError

@Singleton
class UKTaxReturnController @Inject() (
  cc:                 ControllerComponents,
  ukTaxReturnService: UKTaxReturnService,
  authenticate:       AuthAction
)(implicit ec:        ExecutionContext)
    extends BackendController(cc) {

  def submitUKTaxReturn(): Action[UktrSubmission] = authenticate(parse.json[UktrSubmission]).async { implicit request =>
    request.headers.get("X-Pillar2-Id") match {
      case Some(pillar2Id) =>
        ukTaxReturnService
          .submitUKTaxReturn(request.body, pillar2Id)
          .map(convertToResult)
      case None =>
        Future.successful(
          BadRequest(
            Json.toJson(
              SimpleError(
                "400",
                "Missing X-Pillar2-Id header"
              ) // TODO: Does this need to be a 400 or a 500 as the only way this happens is if submissions-api doesn't provde the header
            )
          )
        )
    }
  }

  private def convertToResult(response: HttpResponse): Result =
    response.status match {
      case 201 =>
        response.json.validate[SuccessResponse] match {
          case JsSuccess(success, _) => Ok(Json.toJson(success))
          case JsError(errors) =>
            InternalServerError(
              Json.toJson(
                SimpleError("500", s"Failed to parse success response: $errors")
              )
            )
        }
      case 422 =>
        response.json.validate[ValidationErrors] match {
          case JsSuccess(validationErrors, _) => UnprocessableEntity(Json.toJson(SimpleError(validationErrors.code, validationErrors.text)))
          case JsError(_)                   =>
            //logger.error(s"Failed to parse validation errors: $errors")
            InternalServerError(Json.toJson(SimpleError("500", "Internal server error")))
        }
      case _ =>
        InternalServerError(
          Json.toJson(SimpleError("500", "Internal server error"))
        ) // TODO: This loses a lot of infomation on what the error actually is and we rely on the implicit logs provided by play logging, maybe set this up to decode the message
    }
}
