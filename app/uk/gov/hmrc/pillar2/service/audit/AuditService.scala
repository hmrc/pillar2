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
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.models.audit._
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{AmendResponse, AmendSubscriptionSuccess, SubscriptionResponse}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  auditConnector: AuditConnector
)(using ec:       ExecutionContext)
    extends Logging {

  def auditUpeRegisterWithoutId(
    upeRegistration: UpeRegistration
  )(using hc:        HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      UpeRegisterWithoutIdAuditEvent(
        upeRegistration = upeRegistration
      ).extendedDataEvent
    )

  def auditFmRegisterWithoutId(
    nominatedFilingMember: NominatedFilingMember
  )(using hc:              HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      FmRegisterWithoutIdAuditEvent(
        nominatedFilingMember = nominatedFilingMember
      ).extendedDataEvent
    )

  def auditCreateSubscription(
    subscriptionRequest: RequestDetail,
    responseReceived:    AuditResponseReceived
  )(using hc:            HeaderCarrier): Future[AuditResult] = {
    //TODO - This needs to be fixed as we are loosing failure of response
    val resData = responseReceived.status match {
      case CREATED =>
        val response = responseReceived.responseData.as[SuccessResponse]
        (response.success.plrReference, response.success.processingDate.toString)
      case _ => ("", "")
    }

    auditConnector.sendExtendedEvent(
      CreateSubscriptionAuditEvent(
        subscriptionRequest.upeDetails,
        subscriptionRequest.accountingPeriod,
        subscriptionRequest.upeCorrespAddressDetails,
        subscriptionRequest.primaryContactDetails,
        subscriptionRequest.secondaryContactDetails,
        subscriptionRequest.filingMemberDetails,
        plrReference = resData._1,
        processingDate = resData._2
      ).extendedDataEvent
    )
  }

  def auditReadSubscriptionSuccess(
    plrReference: String,
    responseData: SubscriptionResponse
  )(using hc:     HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ReadSubscriptionSuccessAuditEvent(
        plrReference = plrReference,
        formBundleNumber = responseData.success.formBundleNumber,
        upeDetails = responseData.success.upeDetails,
        upeCorrespAddressDetails = responseData.success.upeCorrespAddressDetails,
        primaryContactDetails = responseData.success.primaryContactDetails,
        secondaryContactDetails = responseData.success.secondaryContactDetails,
        filingMemberDetails = responseData.success.filingMemberDetails,
        accountingPeriod = responseData.success.accountingPeriod,
        accountStatus = responseData.success.accountStatus
      ).extendedDataEvent
    )

  def auditReadSubscriptionFailure(
    plrReference: String,
    status:       Int,
    responseData: JsValue
  )(using hc:     HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ReadSubscriptionFailedAuditEvent(
        plrReference = plrReference,
        responseData = AuditResponseReceived(status = status, responseData = responseData)
      ).extendedDataEvent
    )

  def auditAmendSubscription(
    requestData:  AmendSubscriptionSuccess,
    responseData: AuditResponseReceived
  )(using hc:     HeaderCarrier): Future[AuditResult] = {
    //TODO - This needs to be fixed as we are loosing failure of response
    val resData = responseData.status match {
      case OK =>
        val response = responseData.responseData.as[AmendResponse]
        response.success.processingDate
      case _ => ""
    }
    auditConnector.sendExtendedEvent(
      AmendSubscriptionSuccessAuditEvent(
        requestData.replaceFilingMember,
        requestData.upeDetails,
        requestData.accountingPeriod,
        requestData.upeCorrespAddressDetails,
        requestData.primaryContactDetails,
        requestData.secondaryContactDetails,
        requestData.filingMemberDetails,
        processingDate = resData
      ).extendedDataEvent
    )
  }
}
