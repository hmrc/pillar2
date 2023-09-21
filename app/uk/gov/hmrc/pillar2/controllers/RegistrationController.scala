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

package uk.gov.hmrc.pillar2.controllers

import play.api.libs.json.{JsObject, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.ErrorDetails
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.RegistrationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class RegistrationController @Inject() (
  repository:                RegistrationCacheRepository,
  dataSubmissionService:     RegistrationService,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BasePillar2Controller(cc) {

  def withoutIdUpeRegistrationSubmission(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    getUserAnswers(id).flatMap { userAnswer =>
      dataSubmissionService
        .sendNoIdUpeRegistration(userAnswer)
        .map(handleResult)
    }
  }

  def withoutIdFmRegistrationSubmission(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    getUserAnswers(id).flatMap { userAnswer =>
      dataSubmissionService
        .sendNoIdFmRegistration(userAnswer)
        .map(handleResult)
    }
  }

  def getUserAnswers(id: String)(implicit executionContext: ExecutionContext): Future[UserAnswers] =
    repository.get(id).map { userAnswer =>
      UserAnswers(id = id, data = userAnswer.getOrElse(Json.obj()).as[JsObject])
    }

  private def handleResult(httpResponse: HttpResponse): Result =
    httpResponse.status match {
      case OK =>
        logger.info(s"Received Response body - ${httpResponse.body}")
        Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)

      case BAD_REQUEST =>
        logServerError(httpResponse.body)
        BadRequest(httpResponse.body)

      case FORBIDDEN =>
        logServerError(httpResponse.body)
        Forbidden(httpResponse.body)

      case _ =>
        logServerError(httpResponse.body)
        InternalServerError(httpResponse.body)
    }

  private def logServerError(body: String): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetails])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.error(
          s"Error with Regisration: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}"
        )
      case _ =>
        logger.error("Error with Registration but return is not a valid json")
    }
  }

}
