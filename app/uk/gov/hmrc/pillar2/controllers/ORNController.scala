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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, Pillar2HeaderAction}
import uk.gov.hmrc.pillar2.models.orn.ORNRequest
import uk.gov.hmrc.pillar2.service.ORNService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ORNController @Inject() (
  ornService:                ORNService,
  authenticate:              AuthAction,
  pillar2HeaderExists:       Pillar2HeaderAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitOrn: Action[ORNRequest] = (authenticate andThen pillar2HeaderExists).async(parse.json[ORNRequest]) { implicit request =>
    implicit val pillar2Id: String = request.pillar2Id
    ornService
      .submitOrn(request.body)
      .map(response => Created(Json.toJson(response.success)))
  }

  def amendOrn: Action[ORNRequest] = (authenticate andThen pillar2HeaderExists).async(parse.json[ORNRequest]) { implicit request =>
    implicit val pillar2Id: String = request.pillar2Id
    ornService
      .amendOrn(request.body)
      .map(response => Ok(Json.toJson(response.success)))
  }

  def getOrn(fromDate: String, toDate: String): Action[AnyContent] = (authenticate andThen pillar2HeaderExists).async { implicit request =>
    implicit val pillar2Id: String = request.pillar2Id
    ornService
      .getOrn(LocalDate.parse(fromDate), LocalDate.parse(toDate))
      .map(data => Ok(Json.toJson(data.success)))
  }

}
