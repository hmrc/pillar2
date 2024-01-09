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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.BasePillar2Controller
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestController @Inject() (
  repository:                RegistrationCacheRepository,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BasePillar2Controller(cc) {

  def getAllRecords(max: Int): Action[AnyContent] = Action.async { implicit request =>
    repository
      .getAll(max)
      .map { records =>
        Ok(Json.toJson(records))
      }
      .recover { case e: Exception =>
        InternalServerError(Json.obj("message" -> e.getMessage))
      }
  }

  def getRegistrationData(id: String): Action[AnyContent] = Action.async {
    repository.get(id).map { response =>
      response.map(Ok(_)).getOrElse(NotFound)
    }
  }

  def clearCurrentData(id: String): Action[AnyContent] = Action.async { implicit request =>
    repository
      .remove(id)
      .map { _ =>
        Ok(Json.obj("message" -> "Data cleared successfully"))
      }
      .recover { case e: Exception =>
        InternalServerError(Json.obj("message" -> e.getMessage))
      }
  }

  def clearAllData(): Action[AnyContent] = Action.async { implicit request =>
    repository
      .clearAllData()
      .map { _ =>
        Ok(Json.obj("message" -> "Data cleared successfully"))
      }
      .recover { case e: Exception =>
        InternalServerError(Json.obj("message" -> e.getMessage))
      }

  }

  def deEnrol(): Action[AnyContent] = Action.async { implicit request =>
    ???
  }

  def upsertRecord(id: String): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(jsonData) =>
        repository.upsert(id, jsonData).map { _ =>
          Ok("Record upserted successfully")
        }
      case None =>
        Future.successful(BadRequest("Invalid JSON"))
    }
  }

}
