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

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.minLength
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.subscription.{ReadSubscriptionRequestParameters, SubscriptionRequestParameters}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject() (
  repository:                RegistrationCacheRepository,
  subscriptionService:       SubscriptionService,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BasePillar2Controller(cc) {

  def createSubscription: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    val subscriptionParameters: JsResult[SubscriptionRequestParameters] =
      request.body.validate[SubscriptionRequestParameters]
    subscriptionParameters.fold(
      invalid = _ =>
        Future.successful(
          BadRequest("Subcription parameter is invalid")
        ),
      valid = subs =>
        for {
          userAnswer <- getUserAnswers(subs.id)
          response   <- subscriptionService.sendCreateSubscription(subs.regSafeId, subs.fmSafeId, userAnswer)
        } yield convertToResult(response)(implicitly[Logger](logger))
    )
  }

  implicit val readSubscriptionParamsReads: Reads[ReadSubscriptionRequestParameters] = (
    (__ \ "id").read[String](minLength[String](1)) and
      (__ \ "plrReference").read[String](minLength[String](1))
  )(ReadSubscriptionRequestParameters.apply _)

  def readSubscription(id: String, plrReference: String): Action[AnyContent] = authenticate.async { implicit request =>
    logger.info(s"readSubscription called with id: $id, plrReference: $plrReference")

    val params = ReadSubscriptionRequestParameters(id, plrReference)
    logger.info(s"Parameters created: $params")

    val validationResult = Json.toJson(params).validate[ReadSubscriptionRequestParameters]

    validationResult.fold(
      invalid = _ => {
        logger.warn(s"Validation failed for parameters: $params")
        Future.successful(BadRequest(Json.obj("error" -> "Invalid parameters")))
      },
      valid = validParams => {
        logger.info(s"OLA1 Calling subscriptionService with valid parameters: $validParams")
        try subscriptionService
          .retrieveSubscriptionInformation(validParams.id, validParams.plrReference)
          .map { response =>
            logger.info(s"OLA2 Received response: $response")
            handleHttpResponse(response)
          }
          .recover { case e: Exception =>
            logger.error("OLA3 Error retrieving subscription information", e)
            InternalServerError(Json.obj("error" -> "Error retrieving subscription information"))
          } catch {
          case e: Exception =>
            logger.error("OLA4 Exception thrown before Future was created", e)
            Future.successful(InternalServerError(Json.obj("error" -> "Exception thrown before Future was created")))
        }
      }
    )
  }

  private def handleHttpResponse(response: HttpResponse): Result =
    response.status match {
      case 200 => Ok(Json.obj("message" -> "Success"))
      case 400 => BadRequest(Json.obj("error" -> "Bad request from EIS"))
      case 404 => NotFound(Json.obj("error" -> "Resource not found"))
      case 422 => UnprocessableEntity(Json.obj("error" -> "Unprocessable entity"))
      case 500 => InternalServerError(Json.obj("error" -> "Internal server error"))
      case 503 => ServiceUnavailable(Json.obj("error" -> "Service unavailable"))
      case other =>
        logger.warn(s"Unexpected response: $other")
        InternalServerError(Json.obj("error" -> "Unexpected error occurred"))
    }

  def getUserAnswers(id: String)(implicit executionContext: ExecutionContext): Future[UserAnswers] =
    repository.get(id).map { userAnswer =>
      UserAnswers(id = id, data = userAnswer.getOrElse(Json.obj()).as[JsObject])
    }

}
