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
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

// TODO: remove all occurrences of V2
class SubscriptionServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ScalaFutures {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockedCache)
  }

  private val subscriptionService = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService)

  "sendCreateSubscription" - {
    // TODO: improve descriptions
    "Return successful Http Response" in {
      when(mockSubscriptionConnector.sendCreateSubscriptionInformation(any[RequestDetail]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "Success")))

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe OK
        }
      }
    }

    "Return internal server error in service" in
      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryUncompleteUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    "Return internal server error with response" in {
      when(mockSubscriptionConnector.sendCreateSubscriptionInformation(any[RequestDetail]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")))

      forAll(arbitrary[String], Gen.option(arbitrary[String]), arbitraryAnyIdUpeFmUserAnswers.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
        subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    "must handle LimitedLiabilityPartnership entity type correctly" - {
      "when all required data is present" in {
        val userAnswersGen = userAnswersFromGenerators(arbitraryWithIdUpeFmUserDataLLP)

        when(mockSubscriptionConnector.sendCreateSubscriptionInformation(any[RequestDetail]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, "Success")))

        forAll(arbitrary[String], Gen.option(arbitrary[String]), userAnswersGen.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
          subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswers).map { response =>
            response.status mustBe OK
          }
        }
      }

      "throw exception when company profile is missing" in {
        val llpWithoutCompanyProfile = arbitraryWithIdUpeFmUserDataLLP.arbitrary
        val userAnswersGen           = userAnswersFromGenerators(Arbitrary(llpWithoutCompanyProfile))

        forAll(arbitrary[String], Gen.option(arbitrary[String]), userAnswersGen.arbitrary) { (upeSafeId, fmSafeId, userAnswers) =>
          val modifiedData       = (userAnswers.data \ "upeGRSResponse" \ "partnershipEntityRegistrationData").as[JsObject] - "companyProfile"
          val updatedGrsResponse = userAnswers.data.as[JsObject] ++ Json.obj(
            "upeGRSResponse" -> Json.obj(
              "partnershipEntityRegistrationData" -> modifiedData
            )
          )

          val userAnswersWithoutCompanyProfile = userAnswers.copy(data = updatedGrsResponse)

          intercept[Exception] {
            val result = subscriptionService.sendCreateSubscription(upeSafeId, fmSafeId, userAnswersWithoutCompanyProfile).failed.futureValue
            result.getMessage mustEqual "Malformed company Profile"
          }
        }
      }
    }
  }

  "storeSubscriptionResponseV2 " - {
    "return done if a valid response is received from ETMP" in
      forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponseV2]) { (mockId, plrReference, response) =>
        when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse.apply(status = OK, body = Json.toJson(response).toString)))
        when(mockAuditService.auditReadSubscriptionSuccessV2(any[String](), any[SubscriptionResponseV2]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))
        when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.unit)
        val resultFuture = subscriptionService.storeSubscriptionResponseV2(mockId, plrReference)

        resultFuture.futureValue mustEqual response
      }

    "throw exception if no valid json is received from ETMP" in
      forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
        when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.failed(UnexpectedResponse))

        val resultFuture = subscriptionService.storeSubscriptionResponseV2(mockId, plrReference)

        resultFuture.failed.futureValue mustEqual UnexpectedResponse
      }
  }

  "readSubscriptionDataV2 " - {
    "must return subscription response if a valid response is received from ETMP" in
      forAll(plrReferenceGen, arbitrary[SubscriptionResponseV2]) { (plrReference, response) =>
        val mockResponse = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

        when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(mockResponse))
        when(mockAuditService.auditReadSubscriptionSuccessV2(any[String](), any[SubscriptionResponseV2]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))

        val resultFuture = subscriptionService.readSubscriptionDataV2(plrReference).futureValue
        resultFuture mustEqual mockResponse
      }

    "must return subscription response if a valid response is received from ETMP but with no accounting periods" in
      forAll(plrReferenceGen) { plrReference =>
        val subscriptionSuccess = arbitrary[SubscriptionDataDisplay].sample.get.copy(accountingPeriod = None)
        val response            = SubscriptionResponseV2(subscriptionSuccess)
        val mockResponse        = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

        when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(mockResponse))
        when(mockAuditService.auditReadSubscriptionSuccessV2(any[String](), any[SubscriptionResponseV2]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(AuditResult.Success))

        val resultFuture = subscriptionService.readSubscriptionDataV2(plrReference).futureValue
        resultFuture mustEqual mockResponse
      }

    "must throw exception if no valid json is received from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.failed(UnexpectedResponse))
      when(mockAuditService.auditReadSubscriptionFailure(any[String](), any[Int](), any[JsValue]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))
      val resultFuture = subscriptionService.readSubscriptionDataV2(testPillar2Id)
      resultFuture.failed.futureValue mustEqual UnexpectedResponse
    }

    "must handle LimitedLiabilityPartnership entity type correctly" - {
      "when all required data is present" in
        forAll(plrReferenceGen, arbitrary[SubscriptionResponseV2], arbitraryWithIdUpeFmUserDataLLPV2.arbitrary) { (plrReference, response, _) =>
          val mockResponse = HttpResponse.apply(status = OK, body = Json.toJson(response).toString)

          when(mockSubscriptionConnector.getSubscriptionInformationV2(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
            .thenReturn(Future.successful(mockResponse))
          when(mockAuditService.auditReadSubscriptionSuccessV2(any[String](), any[SubscriptionResponseV2]())(using any[HeaderCarrier]()))
            .thenReturn(Future.successful(AuditResult.Success))

          val resultFuture = subscriptionService.readSubscriptionDataV2(plrReference).futureValue
          resultFuture mustEqual mockResponse
        }
    }
  }

  "sendAmendedDataV2" - {
    "must call amend API, audit, and update cache in case of a successful response" in {
      val testSubscriptionResponseV2                = arbitrarySubscriptionResponseV2.arbitrary.sample.value
      val subscriptionServiceWithStubbedStoreMethod = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService) {
        override def storeSubscriptionResponseV2(id: String, plrReference: String)(using hc: HeaderCarrier): Future[SubscriptionResponseV2] =
          Future.successful(testSubscriptionResponseV2)
      }

      forAll(arbitraryAmendSubscriptionSuccessV2.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        val etmpAmendResponse = AmendResponse(
          AmendSubscriptionSuccessResponse(processingDate = LocalDate.now().toString, formBundleNumber = testFormBundleNumber)
        )
        val fakeAmendResponse = HttpResponse(OK, Json.toJson(etmpAmendResponse).toString())

        when(
          mockSubscriptionConnector.amendSubscriptionInformationV2(any[ETMPAmendSubscriptionSuccessV2]())(using
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        )
          .thenReturn(Future.successful(fakeAmendResponse))

        when(mockAuditService.auditAmendSubscriptionV2(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier]))
          .thenReturn(Future.successful(AuditResult.Success))

        subscriptionServiceWithStubbedStoreMethod.sendAmendedDataV2(id, validAmendObject).futureValue mustBe Done
      }
    }

    "must fail with ETMPValidationError, preserving the error code and text, on a 422 response" in {
      when(
        mockAuditService.auditAmendSubscriptionV2(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier])
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
        mockSubscriptionConnector.amendSubscriptionInformationV2(any[ETMPAmendSubscriptionSuccessV2]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, failureBody.toString())))

      forAll(arbitraryAmendSubscriptionSuccessV2.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedDataV2(id, validAmendObject).failed.futureValue mustEqual
          ETMPValidationError("422", "Request Not Processed")
      }
    }

    "must fail with InvalidJsonError when a 200 response has an unparseable body" in {
      when(mockAuditService.auditAmendSubscriptionV2(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(AuditResult.Success))

      when(
        mockSubscriptionConnector.amendSubscriptionInformationV2(any[ETMPAmendSubscriptionSuccessV2]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      )
        .thenReturn(Future.successful(HttpResponse(OK, JsObject.empty, Map.empty)))

      forAll(arbitraryAmendSubscriptionSuccessV2.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedDataV2(id, validAmendObject).failed.futureValue mustBe a[InvalidJsonError]
      }
    }

    "must fail with ApiInternalServerError on any other non-200 status" in {
      when(
        mockAuditService.auditAmendSubscriptionV2(any[SubscriptionDataAmend], any[AuditResponseReceived])(using any[HeaderCarrier])
      ).thenReturn(Future.successful(AuditResult.Success))

      when(
        mockSubscriptionConnector.amendSubscriptionInformationV2(any[ETMPAmendSubscriptionSuccessV2]())(using
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Json.obj("code" -> "400", "text" -> "Bad Request").toString())))

      forAll(arbitraryAmendSubscriptionSuccessV2.arbitrary, arbMockId.arbitrary) { (validAmendObject, id) =>
        subscriptionService.sendAmendedDataV2(id, validAmendObject).failed.futureValue mustBe ApiInternalServerError
      }
    }
  }

}
