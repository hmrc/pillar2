/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Logger, Logging}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.AuthAction
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.subscription.requests.SubscriptionDataAmend
import uk.gov.hmrc.pillar2.models.subscription.SubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject() (
  userAnswerCache:     RegistrationCacheRepository,
  subscriptionService: SubscriptionService,
  authenticate:        AuthAction,
  cc:                  ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createSubscription: Action[JsValue] = authenticate(parse.json).async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    val subscriptionParameters: JsResult[SubscriptionRequestParameters] =
      request.body.validate[SubscriptionRequestParameters]
    subscriptionParameters
      .fold(
        invalid = error => {
          logger.info(s"[SubscriptionController] createSubscription called $error")

          Future.successful(BadRequest("Subscription parameter is invalid"))
        },

        valid = subscriptionRequestParameters =>
          for {
            userAnswer <- getUserAnswers(subscriptionRequestParameters.id)
            response   <- subscriptionService.sendCreateSubscription(
                          upeSafeId = subscriptionRequestParameters.regSafeId,
                          fmSafeId = subscriptionRequestParameters.fmSafeId,
                          userAnswers = userAnswer
                        )
          } yield convertToResult(response)(using logger: Logger)
      )
      .recoverWith { case exception =>
        logger.error(s"[SubscriptionController] Failed to create subscription for id ${subscriptionParameters.map(_.id)}", exception)
        Future.failed(exception)
      }
  }

  def readSubscription(plrReference: String): Action[AnyContent] = authenticate.async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    subscriptionService
      .readSubscriptionData(plrReference)
      .map(convertToResult(_)(using logger: Logger))
      .recoverWith { case exception =>
        logger.error(s"[SubscriptionController] Failed to read subscription for plrReference: $plrReference", exception)
        Future.failed(exception)
      }
  }

  def readAndCacheSubscription(id: String, plrReference: String): Action[AnyContent] = authenticate.async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    subscriptionService
      .storeSubscriptionDisplayResponse(id, plrReference)
      .map(response => Ok(Json.toJson(response.success)))
      .recoverWith { case exception =>
        logger.error(s"[SubscriptionController] Failed to read and cache subscription V2 for plrReference: $plrReference", exception)
        Future.failed(exception)
      }
  }

  def amendSubscription(id: String): Action[SubscriptionDataAmend] = authenticate(parse.json[SubscriptionDataAmend]).async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    subscriptionService.sendAmendedData(id, request.body).map(_ => Ok)
  }

  def getUserAnswers(id: String)(using executionContext: ExecutionContext): Future[UserAnswers] =
    userAnswerCache
      .get(id)
      .map { userAnswer =>
        UserAnswers(id = id, data = userAnswer.getOrElse(Json.obj()).as[JsObject])

      }
      .recoverWith { case exception =>
        logger.error(s"[SubscriptionController] Failed to amend subscription V2 for id: $id", exception)
        Future.failed(exception)
      }

}
