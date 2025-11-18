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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.AuthAction
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail
import uk.gov.hmrc.pillar2.service.RepaymentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RepaymentController @Inject() (
  repaymentService:       RepaymentService,
  authenticate:           AuthAction,
  cc:                     ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def repaymentsSendRequest: Action[RepaymentRequestDetail] = authenticate(parse.json[RepaymentRequestDetail]).async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    repaymentService.sendRepaymentsData(request.body).map(_ => Created)
  }

}
