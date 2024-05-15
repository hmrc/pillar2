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

package uk.gov.hmrc.pillar2.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

class AuditServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val service =
      new AuditService(mockAuditConnector)
  }

  "RegistrationwithoutId for UPE" - {
    "Send successful RegisterwithoutID for UPE" in new Setup {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitraryUpeRegistration.arbitrary) { upeReg =>
        val result = await(service.auditUpeRegisterWithoutId(upeReg))
        result mustBe AuditResult.Success

      }
    }

  }
  "RegistrationwithoutId for FilingMember" - {
    "Send successful RegisterwithoutID for FM" in new Setup {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitraryNominatedFilingMember.arbitrary) { nominatedFilingMember =>
        val result = await(service.auditFmRegisterWithoutId(nominatedFilingMember))
        result mustBe AuditResult.Success

      }
    }

  }

  "CreateSubscription" - {
    "Send successful createSubscription" in new Setup {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitraryRequestDetail.arbitrary, arbitraryCreateAuditResponseReceived.arbitrary) { (requestDetails, auditResponseReceived) =>
        val result = await(service.auditCreateSubscription(requestDetails, auditResponseReceived))
        result mustBe AuditResult.Success
      }
    }
  }

  "ReadSubscription" - {
    "Send successful readSubscription" in new Setup {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbPlrReference.arbitrary, arbitrarySubscriptionResponse.arbitrary) { (plrRef, response) =>
        val result = await(service.auditReadSubscriptionSuccess(plrRef, response))
        result mustBe AuditResult.Success
      }
    }

  }

  "AmendSubscription" - {
    "Send successful amendSubscription" in new Setup {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitraryAmendSubscriptionSuccess.arbitrary, arbitraryAmendAuditResponseReceived.arbitrary) { (requestDetail, responseDetail) =>
        val result = await(service.auditAmendSubscription(requestDetail, responseDetail))
        result mustBe AuditResult.Success
      }
    }
  }

}
