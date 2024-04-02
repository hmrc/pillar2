
package uk.gov.hmrc.pillar2.controllers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Lang.logger
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionResponse
import uk.gov.hmrc.pillar2.models.hods.{ErrorDetail, ErrorDetails, SourceFaultDetail}
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.subscription.{AmendSubscriptionRequestParameters, MneOrDomestic, SubscriptionRequestParameters}
import uk.gov.hmrc.pillar2.repositories.{ReadSubscriptionCacheRepository, RegistrationCacheRepository}
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
class SubscriptionControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]

  trait Setup {
    val controller =
      new SubscriptionController(
        mockRegistrationCacheRepository,
        mockSubscriptionService,
        mockSubscriptionConnector,
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

  override def afterEach(): Unit = {
    reset(mockSubscriptionConnector, mockRegistrationCacheRepository, mockAuthConnector, mockSubscriptionService)
    super.afterEach()
  }

  "SubscriptionController" - {

    "createSubscription" - {
      "should return BAD_REQUEST when subscriptionRequestParameter is invalid" in new Setup {

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

        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
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

      "return OK when valid data is provided" in {
        forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) { (id: String, plrReference: String, response) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(status = OK, body = Json.toJson(response).toString())))

          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
          val result  = route(application, request).value

          status(result) mustBe OK
        }
      }

      "return NotFound response if no data is found" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, """{}""")))

          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(mockId, plrReference).url)
          val result  = route(application, request).value

          status(result) mustBe NOT_FOUND
        }

      }

      "Return Unprocessable Entity HttpResponse when subscription is unprocessable" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, """{}""")))

          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(mockId, plrReference).url)
          val result  = route(application, request).value

          status(result) mustBe UNPROCESSABLE_ENTITY
        }
      }

      "Return InternalServerError HttpResponse for internal server error" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, """{}""")))

          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(mockId, plrReference).url)
          val result  = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "Return ServiceUnavailable HttpResponse when service is unavailable" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
            .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, """{}""")))

          val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(mockId, plrReference).url)
          val result  = route(application, request).value

          status(result) mustBe SERVICE_UNAVAILABLE
        }
      }

      "should return InternalServerError when an exception occurs" in {
        val id           = "testId"
        val plrReference = "testPlrReference"

        when(mockSubscriptionService.storeSubscriptionResponse(any(), any())(any()))
          .thenReturn(Future.failed(new Exception))

        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
        val result  = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR

      }

    }

    "amendSubscription" - {

      "return OK when valid data is provided" in new Setup {
        forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { userAnswers =>
          stubPutResponse(
            s"/pillar2/subscription",
            OK
          )
          val id = "123"

          val jsonUpdatedAnswers = Json.toJson(userAnswers)(UserAnswers.format)
          when(mockRegistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
            .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier]))
            .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

          val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
          val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
            .withJsonBody(requestJson)

          val resultFuture = route(application, fakeRequest).value

          status(resultFuture) shouldBe OK

        }
      }

      "handle an invalid JSON format in the request" in new Setup {
        val userAnswers = UserAnswers(id, Json.obj())
        val updatedUserAnswers = for {
          u1 <- userAnswers.set(subMneOrDomesticId, MneOrDomestic.Uk)

          u2 <- u1.set(subAddSecondaryContactId, true)
        } yield u2

        stubPutResponse(
          s"/pillar2/subscription",
          OK
        )
        val id = "123"

        updatedUserAnswers match {
          case Success(updatedAnswers) =>
            val jsonUpdatedAnswers = Json.toJson(updatedAnswers)(UserAnswers.format)
            when(mockRegistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
              .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          case Failure(exception) =>
            logger.error("Error creating updated UserAnswers", exception)
        }

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(400, "Invalid subscription response")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) shouldBe BAD_REQUEST
      }

      "handle exceptions thrown by the SubscriptionService" in new Setup {

        val id = "123"

        when(mockRegistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("Service error")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) mustBe INTERNAL_SERVER_ERROR
      }

      "return BadRequest when given invalid subscription parameters" in new Setup {
        forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { userAnswers =>
          stubPutResponse(
            s"/pillar2/subscription",
            OK
          )
          val id = "123"

          val jsonUpdatedAnswers = Json.toJson(userAnswers)(UserAnswers.format)
          when(mockRegistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
            .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier]))
            .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

          val invalidJson = Json.obj("invalidField" -> "invalidValue")
          val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
            .withJsonBody(invalidJson)

          val resultFuture = route(application, fakeRequest).value

          status(resultFuture) mustBe BAD_REQUEST
          contentAsString(resultFuture) must include("Amend Subscription parameter is invalid")
        }
      }

      "fail with IllegalArgumentException when UserAnswers is null" in {
        val id = "123"

        when(mockRegistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        val result = service.extractAndProcess(null)

        whenReady(result.failed, timeout(Span(5, Seconds)), interval(Span(500, Millis))) { e =>
          e mustBe a[IllegalArgumentException]
          e.getMessage must include("UserAnswers cannot be null")
        }
      }

    }
  }
}
