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

import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.actions.AuthAction
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationCacheController @Inject() (
  repository:             RegistrationCacheRepository,
  authenticate:           AuthAction,
  cc:                     ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def save(id: String): Action[AnyContent] = authenticate.async { request =>
    request.body.asJson.map { jsValue =>
      repository.upsert(id, jsValue).map(_ => Ok)
    } getOrElse Future.successful(EntityTooLarge)
  }

  def get(id: String): Action[AnyContent] = authenticate.async { _ =>
    repository.get(id).map { response =>
      response.map(Ok(_)).getOrElse(NotFound)
    }
  }

  def remove(id: String): Action[AnyContent] = authenticate.async { _ =>
    repository.remove(id).map { response =>
      if response then Ok else InternalServerError
    }
  }

  private val javaDateTimeNumberWrites = new Writes[Instant] {
    def writes(d: Instant): JsValue = JsNumber(d.getEpochSecond)
  }

  def lastUpdated(id: String): Action[AnyContent] = authenticate.async { _ =>
    repository.getLastUpdated(id).map { response =>
      response.map { date =>
        Ok(Json.toJson(date)(javaDateTimeNumberWrites))
      } getOrElse NotFound
    }
  }
}
