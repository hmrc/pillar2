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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.controllers.auth.FakeAuthAction
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.{ErrorDetail, ErrorDetails, SourceFaultDetail}
import uk.gov.hmrc.pillar2.models.subscription.SubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.concurrent.Future

class SubscriptionControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val controller =
      new SubscriptionController(
        mockRgistrationCacheRepository,
        mockSubscriptionService,
        mockAuthAction,
        stubControllerComponents()
      )
  }

  val jsData = Json.parse("""{"value": "field"}""")
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRgistrationCacheRepository),
      bind[SubscriptionService].toInstance(mockSubscriptionService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "SubscriptionController" - {

    "createSubscription" - {
      "should return BAD_REQUEST when subscriptionRequestParameter is invalid" in new Setup {

        val request =
          FakeRequest(
            POST,
            routes.SubscriptionController.createSubscription.url
          )
            .withJsonBody(Json.parse("""{"value": "field"}"""))

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

      "should return BAD_REQUEST when come from Bad request come from EIS" in {

        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        val errorDetails = ErrorDetails(
          ErrorDetail(
            DateTime.now().toString,
            Some("xx"),
            "409",
            "CONFLICT",
            "",
            Some(SourceFaultDetail(Seq("a", "b")))
          )
        )
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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

  }
}
