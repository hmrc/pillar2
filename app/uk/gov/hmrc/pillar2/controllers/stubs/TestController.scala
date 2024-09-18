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

package uk.gov.hmrc.pillar2.controllers.stubs

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestController @Inject() (
  repository:                RegistrationCacheRepository,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def getAllRecords(max: Int): Action[AnyContent] = Action.async {
    repository.getAll(max).map { response =>
      Ok(Json.toJson(response))
    }
  }

  def getRegistrationData(id: String): Action[AnyContent] = Action.async {
    repository.get(id).map { response =>
      response.map(Ok(_)).getOrElse(NotFound)
    }
  }

  def clearCurrentData(id: String): Action[AnyContent] = Action.async {
    repository.remove(id).map(_ => Ok)
  }

  def clearAllData(): Action[AnyContent] = Action.async {
    repository.clearAllData().map(_ => Ok)
  }

  def deEnrol(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented("This method is not yet implemented"))
  }
}
