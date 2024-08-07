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

package uk.gov.hmrc.pillar2.service

import org.apache.pekko.Done
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.connectors.RepaymentConnector
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepaymentService @Inject() (
  repaymentConnector: RepaymentConnector
)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendRepaymentsData(rePaymentData: RepaymentRequestDetail)(implicit hc: HeaderCarrier): Future[Done] =
    repaymentConnector.sendRepaymentDetails(rePaymentData).flatMap { response =>
      if (response.status == 201) {
        Future.successful(Done)
      } else {
        Future.failed(UnexpectedResponse)
      }
    }

}
