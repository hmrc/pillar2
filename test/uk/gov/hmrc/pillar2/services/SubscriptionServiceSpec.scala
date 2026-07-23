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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.audit.AuditResponseReceived
import uk.gov.hmrc.pillar2.models.errors.Pillar2Error.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.errors.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.subscription.common.*
import uk.gov.hmrc.pillar2.models.hods.subscription.requests.*
import uk.gov.hmrc.pillar2.models.hods.subscription.responses.{SubscriptionDataDisplay, SubscriptionDisplayResponse}
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ScalaFutures {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockedCache)
  }

  private val subscriptionService = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService)

  "sendCreateSubscription" - {
    "must return OK when the connector responds successfully" in {
      when(
        mockSubscriptionConnector.sendCreateSubscriptionInformation(any[SubscriptionDataCreate]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      )
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.obj().toString)))
      when(mockAuditService.auditCreateSubscription(any[SubscriptionDataCreate](), any[AuditResponseReceived]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).futureValue.status mustBe OK
      }
    }

    "must return INTERNAL_SERVER_ERROR when the user answers are incomplete" in
      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryUncompleteUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).futureValue.status mustBe INTERNAL_SERVER_ERROR
      }

    "must return INTERNAL_SERVER_ERROR when the connector responds with a server error" in {
      when(
        mockSubscriptionConnector.sendCreateSubscriptionInformation(any[SubscriptionDataCreate]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      )
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, Json.obj().toString)))
      when(mockAuditService.auditCreateSubscription(any[SubscriptionDataCreate](), any[AuditResponseReceived]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).futureValue.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "must handle LimitedLiabilityPartnership entity type correctly" - {
      "when all required data is present" in {
        when(
          mockSubscriptionConnector.sendCreateSubscriptionInformation(any[SubscriptionDataCreate]())(using
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        )
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.obj().toString)))
        when(mockAuditService.auditCreateSubscription(any[SubscriptionDataCreate](), any[AuditResponseReceived]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))

        forAll(arbitrary[String], Gen.option(arbitrary[String]), userAnswersFromGenerators(arbitraryWithIdUpeFmUserDataLLP).arbitrary) {
          (upeSafeId, fmSafeId, userAnswers) =>
            subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).futureValue.status mustBe OK
        }
      }

      "must fail when the company profile is missing" in
        forAll(arbitrary[String], Gen.option(arbitrary[String]), userAnswersFromGenerators(arbitraryWithIdUpeFmUserDataLLP).arbitrary) {
          (upeSafeId, fmSafeId, userAnswers) =>
            val partnershipData = (userAnswers.data \ "upeGRSResponse" \ "partnershipEntityRegistrationData").as[JsObject] - "companyProfile"
            val userAnswersWithoutCompanyProfile = userAnswers.copy(
              data = userAnswers.data.as[JsObject] ++ Json.obj(
                "upeGRSResponse" -> Json.obj("partnershipEntityRegistrationData" -> partnershipData)
              )
            )

            val exception = intercept[Exception] {
              subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswersWithoutCompanyProfile)
            }
            exception.getMessage mustEqual "Malformed company Profile"
        }
    }
  }

  "storeSubscriptionResponse" - {
    "return done if a valid response is received from ETMP" in
      forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionDisplayResponse]) { (mockId, plrReference, response) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse.apply(status = OK, body = Json.toJson(response).toString)))
        when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionDisplayResponse]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.unit)
        val resultFuture = subscriptionService.storeSubscriptionDisplayResponse(mockId, plrReference)

        resultFuture.futureValue mustEqual response
      }

    "throw exception if no valid json is received from ETMP" in
      forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
        when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.failed(UnexpectedResponse))

        val resultFuture = subscriptionService.storeSubscriptionDisplayResponse(mockId, plrReference)

        resultFuture.failed.futureValue mustEqual UnexpectedResponse
      }
  }

  "readSubscriptionData" - {
    "must return subscription response if a valid response is received from ETMP" in
      forAll(plrReferenceGen, arbitrary[SubscriptionDisplayResponse]) { (plrReference, response) =>
        val mockResponse = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

        when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(mockResponse))
        when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionDisplayResponse]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))

        val resultFuture = subscriptionService.readSubscriptionData(plrReference).futureValue
        resultFuture mustEqual mockResponse
      }

    "must return subscription response if a valid response is received from ETMP but with no accounting periods" in
      forAll(plrReferenceGen) { plrReference =>
        val subscriptionSuccess = arbitrary[SubscriptionDataDisplay].sample.get.copy(accountingPeriod = None)
        val response            = SubscriptionDisplayResponse(subscriptionSuccess)
        val mockResponse        = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

        when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(mockResponse))
        when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionDisplayResponse]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))

        val resultFuture = subscriptionService.readSubscriptionData(plrReference).futureValue
        resultFuture mustEqual mockResponse
      }

    "must throw exception if no valid json is received from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(UnexpectedResponse))
      when(mockAuditService.auditReadSubscriptionFailure(any[String](), any[Int](), any[JsValue]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))
      val resultFuture = subscriptionService.readSubscriptionData(testPillar2Id)
      resultFuture.failed.futureValue mustEqual UnexpectedResponse
    }

    "must handle LimitedLiabilityPartnership entity type correctly" - {
      "when all required data is present" in
        forAll(plrReferenceGen, arbitrary[SubscriptionDisplayResponse], arbitraryWithIdUpeFmUserDataLLPV2.arbitrary) { (plrReference, response, _) =>
          val mockResponse = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
            .thenReturn(Future.successful(mockResponse))
          when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionDisplayResponse]())(using any[HeaderCarrier]()))
            .thenReturn(Future.successful(AuditResult.Success))

          val resultFuture = subscriptionService.readSubscriptionData(plrReference).futureValue
          resultFuture mustEqual mockResponse
        }
    }
  }

  "sendAmendedData" - {
    "must call amend API, audit, and update cache in case of a successful response" in {
      val testSubscriptionResponse                  = arbitrarySubscriptionDisplayResponse.arbitrary.sample.value
      val subscriptionServiceWithStubbedStoreMethod = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService) {
        override def storeSubscriptionDisplayResponse(id: String, plrReference: String)(using
          hc: HeaderCarrier
        ): Future[SubscriptionDisplayResponse] =
          Future.successful(testSubscriptionResponse)
      }

      forAll(arbitrarySubscriptionDataAmend.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        val etmpAmendResponse = AmendResponse(
          AmendSubscriptionSuccessResponse(processingDate = LocalDate.now().toString, formBundleNumber = testFormBundleNumber)
        )
        val fakeAmendResponse = HttpResponse(OK, Json.toJson(etmpAmendResponse).toString())

        when(
          mockSubscriptionConnector.amendSubscriptionInformation(any[EtmpAmendSubscriptionRequest]())(using
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        )
          .thenReturn(Future.successful(fakeAmendResponse))

        when(mockAuditService.auditAmendSubscription(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier]))
          .thenReturn(Future.successful(AuditResult.Success))

        subscriptionServiceWithStubbedStoreMethod.sendAmendedData(id, validAmendObject).futureValue mustBe Done
      }
    }

    "must fail with ETMPValidationError, preserving the error code and text, on a 422 response" in {
      when(
        mockAuditService.auditAmendSubscription(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier])
      ).thenReturn(Future.successful(AuditResult.Success))

      val failureBody = Json.obj(
        "errorDetail" -> Json.obj(
          "timestamp"         -> "2023-02-14T12:58:44Z",
          "errorCode"         -> "422",
          "errorMessage"      -> "Request Not Processed",
          "source"            -> "Back End",
          "sourceFaultDetail" -> Json.obj("detail" -> Json.arr("001 - Request Not Processed"))
        )
      )

      when(
        mockSubscriptionConnector.amendSubscriptionInformation(any[EtmpAmendSubscriptionRequest]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, failureBody.toString())))

      forAll(arbitrarySubscriptionDataAmend.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedData(id, validAmendObject).failed.futureValue mustEqual
          ETMPValidationError("422", "Request Not Processed")
      }
    }

    "must fail with InvalidJsonError when a 200 response has an unparseable body" in {
      when(mockAuditService.auditAmendSubscription(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(AuditResult.Success))

      when(
        mockSubscriptionConnector.amendSubscriptionInformation(any[EtmpAmendSubscriptionRequest]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      )
        .thenReturn(Future.successful(HttpResponse(OK, JsObject.empty, Map.empty)))

      forAll(arbitrarySubscriptionDataAmend.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedData(id, validAmendObject).failed.futureValue mustBe a[InvalidJsonError]
      }
    }

    "must fail with ApiInternalServerError on any other non-200 status" in {
      when(
        mockAuditService.auditAmendSubscription(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier])
      ).thenReturn(Future.successful(AuditResult.Success))

      when(
        mockSubscriptionConnector.amendSubscriptionInformation(any[EtmpAmendSubscriptionRequest]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Json.obj("code" -> "400", "text" -> "Bad Request").toString())))

      forAll(arbitrarySubscriptionDataAmend.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedData(id, validAmendObject).failed.futureValue mustBe ApiInternalServerError
      }
    }
  }

}
