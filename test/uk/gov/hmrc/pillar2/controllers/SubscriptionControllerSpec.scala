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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Lang.logger
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Logger}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.controllers.auth.FakeAuthAction
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{DashboardInfo, FilingMemberDetails, SubscriptionResponse}
import uk.gov.hmrc.pillar2.models.hods.{ErrorDetail, ErrorDetails, SourceFaultDetail}
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.subscription.{AmendSubscriptionRequestParameters, ExtraSubscription, MneOrDomestic, SubscriptionRequestParameters}
import uk.gov.hmrc.pillar2.models.{UserAnswers, _}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
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

    "amendSubscription" - {

      "return OK when valid data is provided" in new Setup {

        val userAnswers = UserAnswers(id, Json.obj())
        val updatedUserAnswers = for {
          u1 <- userAnswers.set(subMneOrDomesticId, MneOrDomestic.Uk)
          u2 <- u1.set(upeNameRegistrationId, "International Organisation Inc.")
          u3 <- u2.set(subPrimaryContactNameId, "Fred Flintstone")
          u4 <- u3.set(subPrimaryEmailId, "fred.flintstone@aol.com")
          u5 <- u4.set(subSecondaryContactNameId, "Donald Trump")
          u6 <- u5.set(
                  subRegisteredAddressId,
                  NonUKAddress(
                    addressLine1 = "1 High Street",
                    addressLine2 = Some("Egham"),
                    addressLine3 = "Surrey",
                    addressLine4 = Some("N/A"),
                    postalCode = Some("HP13 6TT"),
                    countryCode = "GB"
                  )
                )
          u7 <- u6.set(FmSafeId, "XL6967739016188")
          u8 <- u7.set(
                  subFilingMemberDetailsId,
                  FilingMemberDetails(
                    safeId = "XL6967739016188",
                    customerIdentification1 = Some("1234Z678"),
                    customerIdentification2 = Some("1234567Y"),
                    organisationName = "Domestic Operations Ltd"
                  )
                )
          u9 <- u8.set(
                  subAccountingPeriodId,
                  AccountingPeriod(
                    startDate = LocalDate.parse("2024-01-06"),
                    endDate = LocalDate.parse("2025-04-06"),
                    duetDate = Some(LocalDate.parse("2024-04-06"))
                  )
                )
          u10 <- u9.set(subAccountStatusId, AccountStatus(inactive = true))
          u11 <- u10.set(subSecondaryEmailId, "fred.flintstone@potus.com")
          u12 <- u11.set(NominateFilingMemberId, false)
          u13 <- u12.set(subSecondaryCapturePhoneId, "0115 9700 700")
          u14 <- u13.set(
                   subExtraSubscriptionId,
                   ExtraSubscription(
                     formBundleNumber = Some("119000004320"),
                     crn = Some("12345678"),
                     utr = Some("12345678"),
                     safeId = None,
                     plrReference = None
                   )
                 )
          u15 <- u14.set(subRegistrationDateId, LocalDate.parse("2022-01-31"))
          u16 <- u15.set(
                   fmDashboardId,
                   DashboardInfo(
                     organisationName = "International Organisation Inc.",
                     registrationDate = LocalDate.parse("2022-01-31")
                   )
                 )
          u17 <- u16.set(subPrimaryCapturePhoneId, "0115 9700 700")
          u18 <- u17.set(subPrimaryPhonePreferenceId, true)
          u19 <- u18.set(subSecondaryPhonePreferenceId, true)
          u20 <- u19.set(subAddSecondaryContactId, true)
        } yield u20

        stubPutResponse(
          s"/pillar2/subscription",
          OK
        )
        val id = "123"

        updatedUserAnswers match {
          case Success(updatedAnswers) =>
            val jsonUpdatedAnswers = Json.toJson(updatedAnswers)(UserAnswers.format)
            when(mockRgistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
              .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          case Failure(exception) =>
            logger.error("Error creating updated UserAnswers", exception)
        }

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) shouldBe OK

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
            when(mockRgistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
              .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          case Failure(exception) =>
            logger.error("Error creating updated UserAnswers", exception)
        }

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(400, "Invalid subscription response")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) shouldBe BAD_REQUEST
      }

      "handle exceptions thrown by the SubscriptionService" in new Setup {
        val userAnswers = UserAnswers(id, Json.obj())
        val updatedUserAnswers = for {
          u1 <- userAnswers.set(subMneOrDomesticId, MneOrDomestic.Uk)
          u2 <- u1.set(upeNameRegistrationId, "International Organisation Inc.")
          u3 <- u2.set(subPrimaryContactNameId, "Fred Flintstone")
          u4 <- u3.set(subPrimaryEmailId, "fred.flintstone@aol.com")
          u5 <- u4.set(subSecondaryContactNameId, "Donald Trump")
          u6 <- u5.set(
                  subRegisteredAddressId,
                  NonUKAddress(
                    addressLine1 = "1 High Street",
                    addressLine2 = Some("Egham"),
                    addressLine3 = "Surrey",
                    addressLine4 = Some("N/A"),
                    postalCode = Some("HP13 6TT"),
                    countryCode = "GB"
                  )
                )
          u7 <- u6.set(FmSafeId, "XL6967739016188")
          u8 <- u7.set(
                  subFilingMemberDetailsId,
                  FilingMemberDetails(
                    safeId = "XL6967739016188",
                    customerIdentification1 = Some("1234Z678"),
                    customerIdentification2 = Some("1234567Y"),
                    organisationName = "Domestic Operations Ltd"
                  )
                )
          u9 <- u8.set(
                  subAccountingPeriodId,
                  AccountingPeriod(
                    startDate = LocalDate.parse("2024-01-06"),
                    endDate = LocalDate.parse("2025-04-06"),
                    duetDate = Some(LocalDate.parse("2024-04-06"))
                  )
                )
          u10 <- u9.set(subAccountStatusId, AccountStatus(inactive = true))
          u11 <- u10.set(subSecondaryEmailId, "fred.flintstone@potus.com")
          u12 <- u11.set(NominateFilingMemberId, false)
          u13 <- u12.set(subSecondaryCapturePhoneId, "0115 9700 700")
          u14 <- u13.set(
                   subExtraSubscriptionId,
                   ExtraSubscription(
                     formBundleNumber = Some("119000004320"),
                     crn = Some("12345678"),
                     utr = Some("12345678"),
                     safeId = None,
                     plrReference = None
                   )
                 )
          u15 <- u14.set(subRegistrationDateId, LocalDate.parse("2022-01-31"))
          u16 <- u15.set(
                   fmDashboardId,
                   DashboardInfo(
                     organisationName = "International Organisation Inc.",
                     registrationDate = LocalDate.parse("2022-01-31")
                   )
                 )
          u17 <- u16.set(subPrimaryCapturePhoneId, "0115 9700 700")
          u18 <- u17.set(subPrimaryPhonePreferenceId, true)
          u19 <- u18.set(subSecondaryPhonePreferenceId, true)
          u20 <- u19.set(subAddSecondaryContactId, true)
        } yield u20

        val id = "123"

        updatedUserAnswers match {
          case Success(updatedAnswers) =>
            when(mockRgistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
              .thenReturn(Future.successful(None))

          case Failure(exception) =>
            logger.error("Error creating updated UserAnswers", exception)
        }

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("Service error")))

        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(requestJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) mustBe INTERNAL_SERVER_ERROR
      }

      "return BadRequest when given invalid subscription parameters" in new Setup {

        val userAnswers = UserAnswers(id, Json.obj())
        val updatedUserAnswers = for {
          u1 <- userAnswers.set(subMneOrDomesticId, MneOrDomestic.Uk)
          u2 <- u1.set(upeNameRegistrationId, "International Organisation Inc.")
          u3 <- u2.set(subPrimaryContactNameId, "Fred Flintstone")
          u4 <- u3.set(subPrimaryEmailId, "fred.flintstone@aol.com")
          u5 <- u4.set(subSecondaryContactNameId, "Donald Trump")
          u6 <- u5.set(
                  subRegisteredAddressId,
                  NonUKAddress(
                    addressLine1 = "1 High Street",
                    addressLine2 = Some("Egham"),
                    addressLine3 = "Surrey",
                    addressLine4 = Some("N/A"),
                    postalCode = Some("HP13 6TT"),
                    countryCode = "GB"
                  )
                )
          u7 <- u6.set(FmSafeId, "XL6967739016188")
          u8 <- u7.set(
                  subFilingMemberDetailsId,
                  FilingMemberDetails(
                    safeId = "XL6967739016188",
                    customerIdentification1 = Some("1234Z678"),
                    customerIdentification2 = Some("1234567Y"),
                    organisationName = "Domestic Operations Ltd"
                  )
                )
          u9 <- u8.set(
                  subAccountingPeriodId,
                  AccountingPeriod(
                    startDate = LocalDate.parse("2024-01-06"),
                    endDate = LocalDate.parse("2025-04-06"),
                    duetDate = Some(LocalDate.parse("2024-04-06"))
                  )
                )
          u10 <- u9.set(subAccountStatusId, AccountStatus(inactive = true))
          u11 <- u10.set(subSecondaryEmailId, "fred.flintstone@potus.com")
          u12 <- u11.set(NominateFilingMemberId, false)
          u13 <- u12.set(subSecondaryCapturePhoneId, "0115 9700 700")
          u14 <- u13.set(
                   subExtraSubscriptionId,
                   ExtraSubscription(
                     formBundleNumber = Some("119000004320"),
                     crn = Some("12345678"),
                     utr = Some("12345678"),
                     safeId = None,
                     plrReference = None
                   )
                 )
          u15 <- u14.set(subRegistrationDateId, LocalDate.parse("2022-01-31"))
          u16 <- u15.set(
                   fmDashboardId,
                   DashboardInfo(
                     organisationName = "International Organisation Inc.",
                     registrationDate = LocalDate.parse("2022-01-31")
                   )
                 )
          u17 <- u16.set(subPrimaryCapturePhoneId, "0115 9700 700")
          u18 <- u17.set(subPrimaryPhonePreferenceId, true)
          u19 <- u18.set(subSecondaryPhonePreferenceId, true)
          u20 <- u19.set(subAddSecondaryContactId, true)
        } yield u20

        stubPutResponse(
          s"/pillar2/subscription",
          OK
        )
        val id = "123"

        updatedUserAnswers match {
          case Success(updatedAnswers) =>
            val jsonUpdatedAnswers = Json.toJson(updatedAnswers)(UserAnswers.format)
            when(mockRgistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
              .thenReturn(Future.successful(Some(jsonUpdatedAnswers)))

          case Failure(exception) =>
            logger.error("Error creating updated UserAnswers", exception)
        }

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

        val invalidJson = Json.obj("invalidField" -> "invalidValue")
        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
          .withJsonBody(invalidJson)

        val resultFuture = route(application, fakeRequest).value

        status(resultFuture) mustBe BAD_REQUEST
        contentAsString(resultFuture) must include("Amend Subscription parameter is invalid")
      }

      "fail with IllegalArgumentException when UserAnswers is null" in {

        stubPutResponse(
          s"/pillar2/subscription",
          OK
        )
        val id = "123"

        when(mockRgistrationCacheRepository.get(eqTo(id))(any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

        val result = service.extractAndProcess(null)

        whenReady(result.failed, timeout(Span(5, Seconds)), interval(Span(500, Millis))) { e =>
          e mustBe a[IllegalArgumentException]
          e.getMessage must include("UserAnswers cannot be null")
        }
      }
    }

  }
}
