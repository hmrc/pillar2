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
import uk.gov.hmrc.pillar2.models.hip.ErrorSummary
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UktrSubmission
import uk.gov.hmrc.pillar2.service.UKTaxReturnService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UKTaxReturnController @Inject() (
  cc:                 ControllerComponents,
  ukTaxReturnService: UKTaxReturnService
  //authenticate:       AuthAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def submitUKTaxReturn(): Action[UktrSubmission] = //authenticate(parse.json[UktrSubmission]).async { implicit request =>
    Action.async(parse.json[UktrSubmission]) { implicit request =>
      request.headers.get("X-Pillar2-Id") match {
        case Some(pillar2Id) =>
          ukTaxReturnService
            .submitUKTaxReturn(request.body, pillar2Id)
            .map(convertToApiResult)
        case None =>
          Future.successful(
            BadRequest(
              Json.toJson(
                ErrorSummary(
                  "400",
                  "Missing X-Pillar2-Id header"
                ) // TODO: Does this need to be a 400 or a 500 as the only way this happens is if submissions-api doesn't provde the header
              )
            )
          )
      }
    }
}
