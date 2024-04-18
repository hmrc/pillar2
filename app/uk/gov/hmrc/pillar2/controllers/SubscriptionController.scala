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

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Logger, Logging}
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.subscription.common.AmendSubscriptionSuccess
import uk.gov.hmrc.pillar2.models.subscription.SubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject() (
  userAnswerCache:           RegistrationCacheRepository,
  subscriptionService:       SubscriptionService,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

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
    userAnswerCache.get(id).map { userAnswer =>
      UserAnswers(id = id, data = userAnswer.getOrElse(Json.obj()).as[JsObject])
    }

  def readAndCacheSubscription(id: String, plrReference: String): Action[AnyContent] = authenticate.async { implicit request =>
    for {
      response <- subscriptionService.storeSubscriptionResponse(id, plrReference)
    } yield Ok(Json.toJson(response.success))
  }

  def readSubscription(plrReference: String): Action[AnyContent] = authenticate.async { implicit request =>
    for {
      response <- subscriptionService.readSubscriptionData(plrReference)
    } yield Ok(Json.toJson(response.success))
  }

  def amendSubscription(id: String): Action[AmendSubscriptionSuccess] = authenticate(parse.json[AmendSubscriptionSuccess]).async { implicit request =>
    subscriptionService.sendAmendedData(id, request.body).map(_ => Ok)
  }

}
