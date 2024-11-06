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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{AmendSubscriptionSuccess, SubscriptionResponse}
import uk.gov.hmrc.pillar2.models.subscription.SubscriptionRequestParameters
import uk.gov.hmrc.pillar2.models.{UnexpectedResponse, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.{ReadSubscriptionCacheRepository, RegistrationCacheRepository}
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}
class SubscriptionControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]

  val jsData: JsValue = Json.parse("""{"value": "field"}""")
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRegistrationCacheRepository),
      bind[SubscriptionService].toInstance(mockSubscriptionService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  val service =
    new SubscriptionService(
      mockedCache,
      mockSubscriptionConnector,
      mockAuditService
    )

  @nowarn
  override def afterEach(): Unit = {
    reset(mockSubscriptionConnector, mockRegistrationCacheRepository, mockAuthConnector, mockSubscriptionService)
    super.afterEach()
  }

  "SubscriptionController" - {

    "createSubscription" - {
      "should return BAD_REQUEST when subscriptionRequestParameter is invalid" in {

        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(
            POST,
            routes.SubscriptionController.createSubscription.url
          )
            .withJsonBody(Json.parse("""{"value": "field"}"""))

        val result: Future[Result] = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

      "should return BAD_REQUEST when come from Bad request come from EIS" in {

        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(400, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
        }
      }

      "should return FORBIDDEN when authorisation is invalid" in {
        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(403, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual FORBIDDEN
        }
      }

      "should return SERVICE_UNAVAILABLE when EIS is down" in {
        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(503, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }

      "should return INTERNAL_SERVER_ERROR when EIS fails" in {
        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(BAD_GATEWAY, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }

      "should return CONFLICT when occurs from EIS" in {
        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(409, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual CONFLICT
        }
      }

      "should return NOT_FOUND from EIS" in {
        when(mockRegistrationCacheRepository.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(404, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual NOT_FOUND
        }
      }

    }

    "readAndCacheSubscription" - {
      "return ok with the response if the connector returns successful" in {
        forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) { (id, plrReference, response) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any[String](), any[String]())(any[HeaderCarrier]()))
            .thenReturn(Future.successful(response))
          val request = FakeRequest(GET, routes.SubscriptionController.readAndCacheSubscription(id, plrReference).url)
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(response.success)
        }
      }

      "return UnexpectedResponse if connector returns non-OK response" in {
        when(mockSubscriptionService.storeSubscriptionResponse(any[String](), any[String]())(any[HeaderCarrier]()))
          .thenReturn(Future.failed(UnexpectedResponse))
        val request = FakeRequest(GET, routes.SubscriptionController.readAndCacheSubscription("id", "pillar2Id").url)
        val result  = route(application, request).value
        result.failed.futureValue mustEqual uk.gov.hmrc.pillar2.models.UnexpectedResponse
      }
    }

    "readSubscription" - {
      "return Ok response with json object if the connector returns successful" in {
        forAll(plrReferenceGen, arbitrary[SubscriptionResponse]) { (plrReference, response) =>
          when(mockSubscriptionService.readSubscriptionData(any[String]())(any[HeaderCarrier]()))
            .thenReturn(Future.successful(HttpResponse.apply(status = OK, body = Json.toJson(response.success).toString)))
          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(plrReference).url)
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(response.success)
        }
      }

      "return UnexpectedResponse if connector fails" in {
        when(mockSubscriptionService.readSubscriptionData(any[String]())(any[HeaderCarrier]())).thenReturn(Future.failed(UnexpectedResponse))
        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription("pillar2Id").url)
        val result  = route(application, request).value
        result.failed.futureValue mustEqual uk.gov.hmrc.pillar2.models.UnexpectedResponse
      }
    }

    "amendSubscription" - {

      "return OK when valid data is provided" in {
        forAll(arbMockId.arbitrary, arbitraryAmendSubscriptionSuccess.arbitrary) { (id, amendData) =>
          when(mockSubscriptionService.sendAmendedData(any[String](), any[AmendSubscriptionSuccess]())(any[HeaderCarrier]()))
            .thenReturn(Future.successful(Done))

          val requestJson = Json.toJson(amendData)
          val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription(id).url)
            .withJsonBody(requestJson)
          val resultFuture = route(application, fakeRequest).value
          status(resultFuture) shouldBe OK
        }
      }
      "return bad request if the validation fails on the json payload" in {
        forAll(arbMockId.arbitrary) { id =>
          when(mockSubscriptionService.sendAmendedData(any[String](), any[AmendSubscriptionSuccess]())(any[HeaderCarrier]()))
            .thenReturn(Future.successful(Done))

          val requestJson = Json.obj("invalid" -> "payload")
          val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription(id).url)
            .withJsonBody(requestJson)
          val resultFuture = route(application, fakeRequest).value
          status(resultFuture) shouldBe BAD_REQUEST
        }
      }

    }

  }
}
