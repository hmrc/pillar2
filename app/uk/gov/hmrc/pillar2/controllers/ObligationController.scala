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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.pillar2.connectors.ObligationConnector
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ObligationController @Inject() (
  obligationsConnector:      ObligationConnector,
  authenticate:              AuthAction,
  cc:                        ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getObligation(plrReference: String, dateFrom: String, dateTo: String): Action[AnyContent] = authenticate.async { implicit request =>
    obligationsConnector
      .getObligations(plrReference, LocalDate.parse(dateFrom), LocalDate.parse(dateTo))
      .map(obligation => Ok(Json.toJson(obligation)))
      .recover { case e: Exception =>
        logger.error(s"Failed to retrieve obligation for plrReference: $plrReference", e)
        NotFound
      }
  }

}
