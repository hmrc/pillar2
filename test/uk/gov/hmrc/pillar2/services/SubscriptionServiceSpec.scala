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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionResponse
import uk.gov.hmrc.pillar2.models.subscription.ReadSubscriptionRequestParameters
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val service =
      new SubscriptionService(
        mockRgistrationCacheRepository,
        mockSubscriptionConnector,
        mockCountryOptions
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

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe OK
        }
      }

    }

    "Return internal server error in service" in new Setup {

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUncompletedUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
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

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryNoIdUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        service.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }

  "retrieveSubscriptionInformation" - {

    "Return successful HttpResponse, retrieve subscription information, and upsert data correctly" in new Setup {
      forAll(arbMockId.arbitrary, arbReadSubscriptionRequestParameters.arbitrary, arbitrary[SubscriptionResponse]) {
        (mockId, mockParams, mockSubscriptionResponse) =>
          val expectedHttpResponse = HttpResponse(status = OK, body = Json.toJson(mockSubscriptionResponse).toString())

          val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
          val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
          val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

          when(
            mockSubscriptionConnector
              .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
          )
            .thenReturn(Future.successful(expectedHttpResponse))

          when(mockRgistrationCacheRepository.upsert(any[String], any[JsValue])(any[ExecutionContext]))
            .thenReturn(Future.successful(()))
          val resultFuture = service.retrieveSubscriptionInformation(mockId, mockParams.plrReference)(hc, ec)

          whenReady(resultFuture) { response =>
            response.status mustBe OK
            assert(plrReferenceCaptor.getValue == mockParams.plrReference)
          }
      }
    }

    "handle external connector failure" in new Setup {
      forAll(arbMockId.arbitrary, arbPlrReference.arbitrary) { (mockId: String, mockPlrReference: String) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new Exception("Mock failure")))

        service.retrieveSubscriptionInformation(mockId, mockPlrReference).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          response.body mustBe "Response not received in Subscription"
        }
      }
    }

    "handle data transformation error" in new Setup {

      val malformedHttpResponse = HttpResponse(status = OK, body = "{\"malformed\": \"data\"}")
      forAll(arbMockId.arbitrary, arbPlrReference.arbitrary) { (mockId: String, mockPlrReference: String) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(malformedHttpResponse))

        service.retrieveSubscriptionInformation(mockId, mockPlrReference).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          response.body mustBe "Response not received in Subscription"
        }
      }
    }

    "handle database upsert failure" in new Setup {
      forAll(arbMockId.arbitrary, arbitrary[ReadSubscriptionRequestParameters], arbitrary[SubscriptionResponse]) {
        (mockId, mockPlrReference, mockSubscriptionResponse) =>
          val expectedHttpResponse = HttpResponse(status = OK, body = Json.toJson(mockSubscriptionResponse).toString())
          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          when(mockRgistrationCacheRepository.upsert(any[String], any[JsValue])(any[ExecutionContext]))
            .thenReturn(Future.failed(new Exception("DB upsert error")))

          service.retrieveSubscriptionInformation(mockId, mockPlrReference.plrReference).map { response =>
            response.status mustBe INTERNAL_SERVER_ERROR
            response.body mustBe "Response not received in Subscription"
          }
      }
    }
  }
}
