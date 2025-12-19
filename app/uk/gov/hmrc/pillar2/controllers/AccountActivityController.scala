/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, Pillar2HeaderAction}
import uk.gov.hmrc.pillar2.models.accountactivity.AccountActivityRequest
import uk.gov.hmrc.pillar2.models.errors.Pillar2ApiError
import uk.gov.hmrc.pillar2.service.AccountActivityService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AccountActivityController @Inject() (
  accountActivityService:   AccountActivityService,
  authenticate:             AuthAction,
  checkPillar2HeaderExists: Pillar2HeaderAction,
  cc:                       ControllerComponents
)(using ExecutionContext)
    extends BackendController(cc) {

  def getAccountActivity(dateFrom: String, dateTo: String): Action[AnyContent] =
    (authenticate andThen checkPillar2HeaderExists).async { request =>
      Try(LocalDate.parse(dateFrom))
        .flatMap(from => Try(LocalDate.parse(dateTo)).map(to => (from, to)))
        .map(AccountActivityRequest.apply)
        .fold(
          _ => Future.successful(BadRequest(Json.toJson(Pillar2ApiError(code = "400", message = "Invalid date format.")))),
          queryParams =>
            accountActivityService
              .getAccountActivity(queryParams, request.pillar2Id)(using hc(request))
              .map(success => Ok(Json.toJson(success)))
        )
    }
}
