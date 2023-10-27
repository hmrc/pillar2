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

package uk.gov.hmrc.pillar2.controllers.stubs

import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.controllers.BasePillar2Controller
import uk.gov.hmrc.pillar2.models.subscription.ReadSubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestController @Inject() (
  subscriptionService:       SubscriptionService,
  repository:                RegistrationCacheRepository,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BasePillar2Controller(cc) {

  def getAllRecords(max: Int): Action[AnyContent] = Action.async { implicit request =>
    repository.getAll(max).map { response =>
      Ok(Json.toJson(response))
    }
  }

  def getRegistrationData(id: String): Action[AnyContent] = Action.async { implicit request =>
    repository.get(id).map { response =>
      response.map(Ok(_)).getOrElse(NotFound)
    }
  }

  def clearCurrentData(id: String): Action[AnyContent] = Action.async { implicit request =>
    repository.remove(id).map(_ => Ok)
  }

  def clearAllData(): Action[AnyContent] = Action.async { implicit request =>
    repository.clearAllData().map(_ => Ok)
  }

  def deEnrol(): Action[AnyContent] = Action.async { implicit request =>
    ???
  }

  def readSubscription(id: String, plrReference: String): Action[AnyContent] = authenticate.async { implicit request =>
    logger.info(s"readSubscription called with id: $id, plrReference: $plrReference")

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

}
