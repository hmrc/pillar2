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

package uk.gov.hmrc.pillar2.service.audit

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.models.audit.{AmendSubscriptionFailedAuditEvent, AmendSubscriptionSuccessAuditEvent, AuditResponseReceived, CreateSubscriptionAuditEvent, FmRegisterWithoutIdAuditEvent, NominatedFilingMember, ReadSubscriptionFailedAuditEvent, ReadSubscriptionSuccessAuditEvent, UpeRegisterWithoutIdAuditEvent, UpeRegistration}
import uk.gov.hmrc.pillar2.models.hods.RegisterWithoutIDRequest
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{AmendSubscriptionSuccess, SubscriptionResponse}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  auditConnector: AuditConnector
)(implicit ec:    ExecutionContext)
    extends Logging {

  def auditUpeRegisterWithoutId(
    upeRegistration: UpeRegistration
  )(implicit hc:     HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      UpeRegisterWithoutIdAuditEvent(
        upeRegistration = upeRegistration
      ).extendedDataEvent
    )

  def auditFmRegisterWithoutId(
    nominatedFilingMember: NominatedFilingMember
  )(implicit hc:           HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      FmRegisterWithoutIdAuditEvent(
        nominatedFilingMember = nominatedFilingMember
      ).extendedDataEvent
    )

  def auditCreateSubscription(
    subscriptionRequest: RequestDetail,
    responseReceived:    AuditResponseReceived
  )(implicit hc:         HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      CreateSubscriptionAuditEvent(
        requestData = subscriptionRequest,
        responseData = responseReceived
      ).extendedDataEvent
    )

  def auditReadSubscriptionSuccess(
    plrReference: String,
    responseData: SubscriptionResponse
  )(implicit hc:  HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ReadSubscriptionSuccessAuditEvent(
        plrReference = plrReference,
        responseData = responseData
      ).extendedDataEvent
    )

  def auditReadSubscriptionFailure(
    plrReference: String,
    responseData: AuditResponseReceived
  )(implicit hc:  HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ReadSubscriptionFailedAuditEvent(
        plrReference = plrReference,
        responseData = responseData
      ).extendedDataEvent
    )

  def auditAmendSubscription(
    requestData:  AmendSubscriptionSuccess,
    responseData: AuditResponseReceived
  )(implicit hc:  HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      AmendSubscriptionSuccessAuditEvent(
        requestData = requestData,
        responseData = responseData
      ).extendedDataEvent
    )

}
