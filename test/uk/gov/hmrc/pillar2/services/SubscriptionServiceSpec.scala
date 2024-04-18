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

import akka.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionResponse
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future
class SubscriptionServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockedCache)
  }

  private val service = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService)

  "sendCreateSubscription" - {
    "Return successful Http Response" in {
      when(
        mockSubscriptionConnector
          .sendCreateSubscriptionInformation(any())(any(), any())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(OK, "Success")
        )
      )

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe OK
        }
      }

    }

    "Return internal server error in service" in {

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryUncompleteUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    "Return internal server error with response" in {
      when(
        mockSubscriptionConnector
          .sendCreateSubscriptionInformation(any())(any(), any())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")
        )
      )

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }

  "storeSubscriptionResponse " - {

    "return done if a valid response is received from ETMP" in {

      forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) { (mockId, plrReference, response) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any())(any(), any())).thenReturn(Future.successful(response))
        when(mockAuditService.auditReadSubscriptionSuccess(any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockedCache.upsert(any(), any())(any())).thenReturn(Future.unit)
        val resultFuture = service.storeSubscriptionResponse(mockId, plrReference)

        resultFuture.futureValue mustEqual response
      }
    }

    "throw exception if no valid json is received from ETMP" in {
      forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any())(any(), any())).thenReturn(Future.failed(UnexpectedResponse))

        val resultFuture = service.storeSubscriptionResponse(mockId, plrReference)

        resultFuture.failed.futureValue mustEqual uk.gov.hmrc.pillar2.models.UnexpectedResponse
      }
    }
  }

  "readSubscriptionData " - {

    "return subscription response if a valid response is received from ETMP" in {

      forAll(plrReferenceGen, arbitrary[SubscriptionResponse]) { (plrReference, response) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any())(any(), any())).thenReturn(Future.successful(response))
        when(mockAuditService.auditReadSubscriptionSuccess(any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
        val resultFuture = service.readSubscriptionData(plrReference)
        resultFuture.futureValue mustEqual response
      }
    }

    "throw exception if no valid json is received from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionInformation(any())(any(), any())).thenReturn(Future.failed(UnexpectedResponse))
      val resultFuture = service.readSubscriptionData("plrReference")
      resultFuture.failed.futureValue mustEqual uk.gov.hmrc.pillar2.models.UnexpectedResponse
    }
  }
  "sendAmendedData" - {
    "call amend API and delete cache in case of a successful response" in {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "Success")))

      forAll(arbitraryAmendSubscriptionSuccess.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        service.sendAmendedData(id, validAmendObject).map { response =>
          response mustBe Done
        }
      }
    }
    "return failure in case of an unusual json response" in {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, None)))

      forAll(arbitraryAmendSubscriptionSuccess.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        service.sendAmendedData(id, validAmendObject).map { response =>
          response mustBe uk.gov.hmrc.pillar2.models.JsResultError
        }
      }
    }

    "return failure in case of a non-200 response" in {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "Bad Request")))

      forAll(arbitraryAmendSubscriptionSuccess.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        service.sendAmendedData(id, validAmendObject).map { response =>
          response mustBe uk.gov.hmrc.pillar2.models.UnexpectedResponse
        }
      }
    }

  }

}
