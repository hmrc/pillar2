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
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.subscription.{AmendSubscriptionRequestParameters, ReadSubscriptionRequestParameters, SubscriptionRequestParameters}
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
      invalid = error => {
        logger.info(s"SubscriptionController - createSubscription called $error")

        Future.successful(
          BadRequest("Subscription parameter is invalid")
        )
      },
      valid = subs =>
        for {
          userAnswer <- getUserAnswers(subs.id)
          response   <- subscriptionService.sendCreateSubscription(subs.regSafeId, subs.fmSafeId, userAnswer)
        } yield convertToResult(response)(implicitly[Logger](logger))
    )
  }

  def getUserAnswers(id: String)(implicit executionContext: ExecutionContext): Future[UserAnswers] =
    repository.get(id).map { userAnswer =>
      UserAnswers(id = id, data = userAnswer.getOrElse(Json.obj()).as[JsObject])
    }

  def readSubscription(id: String, plrReference: String): Action[AnyContent] = authenticate.async { implicit request =>
    logger.info(s"readSubscription called with id: $id, plrReference: $plrReference")
    // repository.upsert(id, Json.toJson(plrReference))
    val paramsJson = Json.obj("id" -> id, "plrReference" -> plrReference)

    paramsJson.validate[ReadSubscriptionRequestParameters] match {
      case JsSuccess(validParams, _) =>
        logger.info(s"Calling subscriptionService with valid parameters: $validParams")
        try subscriptionService
          .retrieveSubscriptionInformation(validParams.id, validParams.plrReference)
          .map { subscriptionResponse =>
            logger.info(s"Received response: $subscriptionResponse")
            Ok(Json.toJson(subscriptionResponse))
          }
          .recover { case e: Exception =>
            logger.error("Error retrieving subscription information", e)
            InternalServerError(Json.obj("error" -> "Error retrieving subscription information"))
          } catch {
          case e: Exception =>
            logger.error("Exception thrown before Future was created", e)
            Future.successful(InternalServerError(Json.obj("error" -> "Exception thrown before Future was created")))
        }

      case JsError(errors) =>
        logger.warn(s"Validation failed for parameters: $paramsJson with errors: $errors")
        Future.successful(BadRequest(Json.obj("error" -> "Invalid parameters")))
    }
  }

  def amendSubscription: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    val subscriptionParameters = request.body.validate[AmendSubscriptionRequestParameters]

    subscriptionParameters.fold(
      invalid = error => {
        logger.info(s"SubscriptionController - amendSubscription called with error: $error")
        Future.successful(BadRequest("Amend Subscription parameter is invalid"))
      },
      valid = subs =>
        getUserAnswers(subs.id).flatMap { typedUserAnswers =>
          subscriptionService
            .extractAndProcess(typedUserAnswers)
            .map { response =>
              convertToResult(response)(implicitly[Logger](logger))
            }
            .recover { case ex: Throwable =>
              logger.error("An error occurred during subscription processing", ex)
              InternalServerError("Internal server error occurred")
            }
        }
    )
  }

}
