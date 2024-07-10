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
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.RepaymentConnector
import uk.gov.hmrc.pillar2.models.audit.AuditResponseReceived
import uk.gov.hmrc.pillar2.models.hods.repayment.common.RepaymentResponse
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail
import uk.gov.hmrc.pillar2.models.{JsResultError, UnexpectedResponse}
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.pillar2.service.audit.AuditService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepaymentService @Inject() (
  repository:         ReadSubscriptionCacheRepository,
  repaymentConnector: RepaymentConnector,
  auditService:       AuditService
)(implicit
  ec: ExecutionContext
) extends Logging {

  private val subscriptionError = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription"))

  def sendRepaymentsData(id: String, amendData: RepaymentRequestDetail)(implicit hc: HeaderCarrier): Future[Done] =
    repaymentConnector.sendRepaymentInformation(amendData).flatMap { response =>
      if (response.status == 200) {
        auditService.auditCreateRepayment(requestData = amendData, responseReceived = AuditResponseReceived(response.status, response.json))
        response.json.validate[RepaymentResponse] match {
          case JsSuccess(result, _) =>
            logger.info(
              s"Successful response received for repayment  for form  at ${result.success.processingDate}"
            )
            repository.remove(id)
            Future.successful(Done)
          case _ => Future.failed(JsResultError)
        }
      } else {
        logger.info(
          s"Unsuccessful response received for repayments  with ${response.status} status and body: ${response.body} "
        )
        auditService.auditCreateRepayment(requestData = amendData, responseReceived = AuditResponseReceived(response.status, response.json))
        // auditService.auditAmendSubscription(requestData = amendData, responseData = AuditResponseReceived(response.status, response.json))
        Future.failed(UnexpectedResponse)
      }
    }

}
