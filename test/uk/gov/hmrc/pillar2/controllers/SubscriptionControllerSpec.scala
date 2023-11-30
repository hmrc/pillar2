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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
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
import uk.gov.hmrc.pillar2.models.subscription.AmendSubscriptionRequestParameters
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
    /*
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
          (id: String, plrReference: String, mockSubscriptionResponse: SubscriptionResponse) =>
            stubResponse(
              s"/pillar2/subscription/$plrReference",
              OK
            )

            when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Json.toJson(mockSubscriptionResponse)))

            when(mockRgistrationCacheRepository.upsert(any[String], any[JsValue])(any[ExecutionContext]))
              .thenReturn(Future.successful(()))

            val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
            val result  = route(application, request).value

            status(result) mustBe OK
        }
      }

      "return NotFound HttpResponse when subscription information is not found" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val logger = Logger(this.getClass)

          when(mockSubscriptionConnector.getSubscriptionInformation(plrReference))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

          logger.debug(s"Mock set for getSubscriptionInformation with plrReference: $plrReference")

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj("error" -> "Error response from service with status: 404 and body: ")
          }
        }
      }
      "Return UnprocessableEntity HttpResponse when subscription is unprocessable" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = UNPROCESSABLE_ENTITY, body = Json.obj("error" -> "Unprocessable entity").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $UNPROCESSABLE_ENTITY and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "Return InternalServerError HttpResponse for internal server error" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> "Internal server error").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $INTERNAL_SERVER_ERROR and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "Return ServiceUnavailable HttpResponse when service is unavailable" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, body = Json.obj("error" -> "Service unavailable").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj("error" -> s"Error response from service with status: $SERVICE_UNAVAILABLE and body: ${expectedHttpResponse.body}")
          }
        }
      }

      "Return InternalServerError HttpResponse for unexpected response" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val unexpectedErrorMessage = "Unexpected error occurred"
          val expectedHttpResponse =
            HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> unexpectedErrorMessage).toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $INTERNAL_SERVER_ERROR and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "should return InternalServerError when an exception occurs" in new Setup {
        val id           = "testId"
        val plrReference = "testPlrReference"

        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new Exception("Test exception")))

        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
        val result  = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Error retrieving subscription information")
      }

      "return InternalServerError when an exception is thrown synchronously" in new Setup {
        val id           = "testId"
        val plrReference = "testPlrReference"

        // Throw an exception synchronously when the method is called
        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenAnswer(new Answer[Future[HttpResponse]] {
            override def answer(invocation: InvocationOnMock): Future[HttpResponse] =
              throw new Exception("Synchronous exception")
          })

        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
        val result  = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Exception thrown before Future was created")
      }

      "respond with InternalServerError when an exception is thrown synchronously" in new Setup {
        val id              = "testId"
        val plrReference    = "testPlrReference"
        val validParamsJson = Json.obj("id" -> id, "plrReference" -> plrReference)
        val fakeRequest = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
          .withBody(validParamsJson)

        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenThrow(new RuntimeException("Synchronous exception"))

        val result = route(application, fakeRequest).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Exception thrown before Future was created")
      }

      "return an InternalServerError when the service call fails" in new Setup {
        val validParamsJson = Json.obj("id" -> "validId", "plrReference" -> "validPlrReference")

        when(
          mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.failed(new RuntimeException("Service call failed")))

        val fakeRequest = FakeRequest(GET, routes.SubscriptionController.readSubscription("validId", "validPlrReference").url)
          .withJsonBody(validParamsJson)

        val result = route(application, fakeRequest).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Error retrieving subscription information")
      }

    }
     */

    "amendSubscription" - {
//      "handle a valid request and successful response" in new Setup {
//        forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { userAnswers =>
//          // Update the mock to return the specific UserAnswers object
//          when(mockRgistrationCacheRepository.get(userAnswers.id))
//            .thenReturn(Future.successful(Some(Json.toJson(userAnswers.data))))
//
//          // Ensure the mockSubscriptionService is called with the expected UserAnswers
//          when(mockSubscriptionService.extractAndProcess(userAnswers))
//            .thenReturn(Future.successful(HttpResponse(200, "")))
//
//          val requestJson = Json.toJson(AmendSubscriptionRequestParameters(userAnswers.id))
//          val fakeRequest = FakeRequest(POST, routes.SubscriptionController.amendSubscription.url)
//            .withJsonBody(requestJson)
//
//          val result = route(application, fakeRequest).value
//
//          status(result) mustBe OK
//        }
//      }

//      "handle a valid request and successful response" in new Setup {
//        // Generate a realistic userAnswersJson
//        val userAnswersJson = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
//          .getOrElse(fail("Unable to generate UserAnswers"))
//          .data
//
//        val userAnswers = UserAnswers("testId", userAnswersJson, Instant.now)
//
//        when(controller.getUserAnswers(any[String])).thenReturn(Future.successful(userAnswers))
//
//        // Mock the repository to return the expected UserAnswers JSON wrapped in an Option
//        when(mockRgistrationCacheRepository.get("testId"))
//          .thenReturn(Future.successful(Some(userAnswersJson)))
//
//        // Mock the service call with the specific UserAnswers
//        when(mockSubscriptionService.extractAndProcess(userAnswers))
//          .thenReturn(Future.successful(HttpResponse(200, "")))
//
//        val requestJson = Json.toJson(AmendSubscriptionRequestParameters("testId"))
//        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url).withJsonBody(requestJson)
//
//        val result = route(application, fakeRequest).value
//
//        status(result) mustBe OK
//      }

      "handle a valid request and successful response" in new Setup {

        val generatedUserAnswers = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
          .getOrElse(fail("Failed to generate valid UserAnswers for testing"))
        val validUserAnswersData = generatedUserAnswers.data
        val testId               = generatedUserAnswers.id

        implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
        implicit val hc: HeaderCarrier    = HeaderCarrier()

        stubPutResponse(
          "/pillar2/subscription/amend-subscription",///subscription/amend-subscription
          OK
        )

        when(mockRgistrationCacheRepository.get(eqTo(testId))(any[ExecutionContext]))
          .thenReturn(Future.successful(Some(validUserAnswersData)))

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(eqTo(hc), eqTo(ec)))
          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(testId))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val result = route(application, fakeRequest).value

        status(result) mustBe OK
      }

      /*
      "handle an invalid JSON format in the request" in {
        val request = FakeRequest(POST, "/amend-subscription")
          .withJsonBody(Json.obj("invalid" -> "data"))
        val result = controller.amendSubscription(request)

        status(result) mustBe BAD_REQUEST
        // Assert the response content if necessary
      }

"handle exceptions thrown by the SubscriptionService" in {
        when(mockRegistrationCacheRepository.get(any[String]))
          .thenReturn(Future.successful(Some(Json.obj())))
        when(mockSubscriptionService.extractAndProcess(any[UserAnswers]))
          .thenReturn(Future.failed(new RuntimeException("Service error")))

        val request = FakeRequest(POST, "/amend-subscription")
          .withJsonBody(Json.toJson(AmendSubscriptionRequestParameters("testId", JsObject(Seq()))))
        val result = controller.amendSubscription(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        // Additional assertions for the error message
      }


       */

    }

  }
}
