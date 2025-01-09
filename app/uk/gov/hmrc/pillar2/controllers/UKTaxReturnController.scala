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

package uk.gov.hmrc.pillar2.controllers

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, Pillar2HeaderAction}
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UKTRSubmission
import uk.gov.hmrc.pillar2.service.UKTaxReturnService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UKTaxReturnController @Inject() (
  cc:                  ControllerComponents,
  ukTaxReturnService:  UKTaxReturnService,
  pillar2HeaderExists: Pillar2HeaderAction,
  authenticate:        AuthAction
)(implicit ec:         ExecutionContext)
    extends BackendController(cc) {

  def submitUKTaxReturn(): Action[UKTRSubmission] = (authenticate andThen pillar2HeaderExists).async(parse.json[UKTRSubmission]) { implicit request =>
    implicit val pillar2Id: String = request.pillar2Id
    ukTaxReturnService
      .submitUKTaxReturn(request.body)
      .map(response => Created(Json.toJson(response.success)))
  }

  def amendUKTaxReturn(): Action[UKTRSubmission] = (authenticate andThen pillar2HeaderExists).async(parse.json[UKTRSubmission]) { implicit request =>
    implicit val pillar2Id: String = request.pillar2Id
    ukTaxReturnService
      .amendUKTaxReturn(request.body)
      .map(response => Ok(Json.toJson(response.success)))
  }
}
