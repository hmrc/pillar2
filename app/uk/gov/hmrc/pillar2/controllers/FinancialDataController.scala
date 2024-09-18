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

import cats.syntax.eq._
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.service.FinancialService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FinancialDataController @Inject() (
  financialService:          FinancialService,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getTransactionHistory(plrReference: String, dateFrom: String, dateTo: String): Action[AnyContent] = authenticate.async { implicit request =>
    financialService.getTransactionHistory(plrReference, LocalDate.parse(dateFrom), LocalDate.parse(dateTo)).map {
      case Right(value)                                                                         => Ok(Json.toJson(value))
      case Left(error) if error.code === "NOT_FOUND"                                            => NotFound(Json.toJson(error))
      case Left(error) if error.code === "SERVER_ERROR" || error.code === "SERVICE_UNAVAILABLE" => FailedDependency(Json.toJson(error))
      case Left(error)                                                                          => BadRequest(Json.toJson(error))
    }
  }

}
