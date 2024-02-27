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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.JsResultError
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionResponse
import uk.gov.hmrc.pillar2.models.subscription.ReadSubscriptionRequestParameters
import uk.gov.hmrc.pillar2.service.SubscriptionService
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}
class SubscriptionServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val service =
      new SubscriptionService(
        mockRgistrationCacheRepository,
        mockSubscriptionConnector,
        mockAuditService
      )
  }

  "sendCreateSubscription" - {
    "Return successful Http Response" in new Setup {
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

    "Return internal server error in service" in new Setup {

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryUncompleteUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    "Return internal server error with response" in new Setup {
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

  "retrieveSubscriptionInformation" - {

    "return done if a valid response is received from ETMP" in new Setup {
      forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) { (mockId, plrReference, response) =>
        when(
          mockSubscriptionConnector
            .getSubscriptionInformation(any())(any(), any())
        ).thenReturn(Future.successful(HttpResponse(status = OK, body = Json.toJson(response).toString())))
        when(mockAuditService.auditReadSubscriptionSuccess(any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockRgistrationCacheRepository.upsert(any(), any())(any())).thenReturn(Future.unit)
        val resultFuture = service.processReadSubscriptionResponse(mockId, plrReference)

        resultFuture.futureValue mustEqual Done
      }
    }

    "throw exception if no valid json is received from ETMP" in new Setup {
      forAll(arbMockId.arbitrary, arbPlrReference.arbitrary) { (mockId: String, mockPlrReference: String) =>
        when(
          mockSubscriptionConnector
            .getSubscriptionInformation(any())(any(), any())
        ).thenReturn(Future.successful(HttpResponse(status = OK, body = Json.obj("something" -> "anotherThing").toString())))

        val resultFuture = service.processReadSubscriptionResponse(mockId, mockPlrReference)

        resultFuture.failed.futureValue mustEqual uk.gov.hmrc.pillar2.models.JsResultError
      }
    }
  }

  "amendSubscription" - {
    "process valid UserAnswers and handle successful amendment" in new Setup {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "Success")))

      forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { validUserAnswers =>
        service.extractAndProcess(validUserAnswers).map { response =>
          response.status mustBe OK
        }
      }
    }

    "handle incomplete UserAnswers resulting in no amendment call" in new Setup {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(BAD_REQUEST, "Bad Request")))

      forAll(arbitraryIncompleteAmendSubscriptionUserAnswers.arbitrary) { invalidUserAnswers =>
        service.extractAndProcess(invalidUserAnswers).map { response =>
          response.status mustBe BAD_REQUEST
        }
      }
    }

    "handle failure response from SubscriptionConnector" in new Setup {

      when(mockSubscriptionConnector.amendSubscriptionInformation(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")))

      forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { validUserAnswers =>
        service.extractAndProcess(validUserAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

  }

}
