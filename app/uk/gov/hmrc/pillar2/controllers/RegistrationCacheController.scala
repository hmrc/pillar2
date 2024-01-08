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

import org.joda.time.DateTime
import play.api.libs.json.{JsNumber, JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.utils.SessionIdHelper

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationCacheController @Inject() (
  repository:                RegistrationCacheRepository,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext, hc: HeaderCarrier)
    extends BasePillar2Controller(cc) {

  def save(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    request.body.asJson.map { jsValue =>
      repository.upsert(id, jsValue).map(_ => Ok)
    } getOrElse Future.successful(EntityTooLarge)
  }

  def get(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    logger.debug(s"[Session ID: ${SessionIdHelper.sessionId(hc)}] - controllers.RegistrationCacheController.get: Authorised Request " + id)
    repository.get(id).map { response =>
      logger.debug(
        s"[Session ID: ${SessionIdHelper.sessionId(hc)}] - controllers.RegistrationCacheController.get: Response for request Id $id is $response"
      )
      response.map(Ok(_)).getOrElse(NotFound)
    }
  }

  def remove(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    repository.remove(id).map { response =>
      if (response) Ok else InternalServerError
    }
  }

  private val jodaDateTimeNumberWrites = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsNumber(d.getMillis)
  }

  def lastUpdated(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    logger.debug("[Session ID: ${SessionIdHelper.sessionId(hc)}] - controllers.RegistrationCacheController.lastUpdated: Authorised Request " + id)
    repository.getLastUpdated(id).map { response =>
      logger.debug("[Session ID: ${SessionIdHelper.sessionId(hc)}] - controllers.RegistrationCacheController.lastUpdated: Response " + response)
      response.map { date =>
        Ok(Json.toJson(date)(jodaDateTimeNumberWrites))
      } getOrElse NotFound
    }
  }
}
