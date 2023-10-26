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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
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
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionResponse
import uk.gov.hmrc.pillar2.models.hods.{ErrorDetail, ErrorDetails, SourceFaultDetail}
import uk.gov.hmrc.pillar2.models.subscription.SubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.concurrent.{ExecutionContext, Future}

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

  val service =
    new SubscriptionService(
      mockRgistrationCacheRepository,
      mockSubscriptionConnector,
      mockCountryOptions
    )

  override def afterEach(): Unit = {
    reset(mockSubscriptionConnector, mockRgistrationCacheRepository, mockAuthConnector, mockSubscriptionService)
    super.afterEach()
  }

  "SubscriptionController" - {

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

      "readSubscription" - {

        "return OK when valid data is provided" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) {
            (id: String, plrReference: String, mockSubscriptionResponse) =>
              stubResponse(
                s"/pillar2/subscription/$plrReference",
                OK
              )
              val expectedHttpResponse = HttpResponse(status = OK, body = Json.toJson(mockSubscriptionResponse).toString())

              when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
                .thenReturn(Future.successful(expectedHttpResponse))

              when(mockRgistrationCacheRepository.upsert(any[String], any[JsValue])(any[ExecutionContext]))
                .thenReturn(Future.successful(()))

              val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
              val result  = route(application, request).value

              status(result) mustBe OK
          }
        }

        "Return NotFound HttpResponse when subscription information is not found" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
            val expectedHttpResponse = HttpResponse(status = NOT_FOUND, body = Json.obj("error" -> "Resource not found").toString())

            val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
            val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
            val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

            when(
              mockSubscriptionConnector
                .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
            )
              .thenReturn(Future.successful(expectedHttpResponse))

            val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

            whenReady(resultFuture) { response =>
              response.status mustBe NOT_FOUND
//              contentAsJson(response) mustBe Json.obj("error" -> "Resource not found")
//              assert(plrReferenceCaptor.getValue == plrReference)
            }
          }
        }

        "Return UnprocessableEntity HttpResponse when subscription is unprocessable" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
            val expectedHttpResponse = HttpResponse(status = UNPROCESSABLE_ENTITY, body = Json.obj("error" -> "Unprocessable entity").toString())

            val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
            val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
            val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

            when(
              mockSubscriptionConnector
                .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
            )
              .thenReturn(Future.successful(expectedHttpResponse))

            val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

            whenReady(resultFuture) { response =>
              response.status mustBe UNPROCESSABLE_ENTITY
              response.body mustBe Json.obj("error" -> "Unprocessable entity").toString()
            }
          }
        }

        "Return InternalServerError HttpResponse for internal server error" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
            val expectedHttpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> "Internal server error").toString())

            val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
            val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
            val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

            when(
              mockSubscriptionConnector
                .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
            )
              .thenReturn(Future.successful(expectedHttpResponse))

            val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

            whenReady(resultFuture) { response =>
              response.status mustBe INTERNAL_SERVER_ERROR
              response.body mustBe Json.obj("error" -> "Internal server error").toString()
            }
          }
        }

        "Return ServiceUnavailable HttpResponse when service is unavailable" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
            val expectedHttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, body = Json.obj("error" -> "Service unavailable").toString())

            val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
            val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
            val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

            when(
              mockSubscriptionConnector
                .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
            )
              .thenReturn(Future.successful(expectedHttpResponse))

            val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

            whenReady(resultFuture) { response =>
              response.status mustBe SERVICE_UNAVAILABLE
              response.body mustBe Json.obj("error" -> "Service unavailable").toString()
            }
          }
        }

        "Return InternalServerError HttpResponse for unexpected response" in new Setup {
          forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
            val expectedHttpResponse =
              HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> "Unexpected error occurred").toString())

            val plrReferenceCaptor     = ArgumentCaptor.forClass(classOf[String])
            val headerCarrierCaptor    = ArgumentCaptor.forClass(classOf[HeaderCarrier])
            val executionContextCaptor = ArgumentCaptor.forClass(classOf[ExecutionContext])

            when(
              mockSubscriptionConnector
                .getSubscriptionInformation(plrReferenceCaptor.capture())(headerCarrierCaptor.capture(), executionContextCaptor.capture())
            )
              .thenReturn(Future.successful(expectedHttpResponse))

            val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

            whenReady(resultFuture) { response =>
              response.status mustBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
    }
  }
}
