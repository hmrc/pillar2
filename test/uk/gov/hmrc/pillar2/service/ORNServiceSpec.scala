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

package uk.gov.hmrc.pillar2.service

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.{BaseSpec, ORNDataFixture}
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.hip._
import uk.gov.hmrc.pillar2.models.orn.{ORNRequest, ORNSuccess, ORNSuccessResponse}

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class ORNServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ORNDataFixture {

  val service = new ORNService(mockOrnConnector)

  "submitOrn" - {
    "should return SuccessResponse for valid ornRequest (201)" in {
      val successResponse = ORNSuccessResponse(ORNSuccess(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "123456789012345"))
      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpCreated))

      val result = service.submitOrn(ornRequest).futureValue
      result mustBe successResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = UnprocessableFailureResponse(UnprocessableFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.submitOrn(ornRequest))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(201, "{invalid json}")

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.submitOrn(ornRequest))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-201/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError] {
        await(service.submitOrn(ornRequest))
      }
    }
  }

  "getOrn" - {
    "should return SuccessResponse when orn exists (200)" in {
      when(
        mockOrnConnector
          .getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      ).thenReturn(Future.successful(HttpResponse(OK, Json.toJson(ornResponse).toString)))

      val result = service.getOrn(fromDate, toDate).futureValue
      result mustBe ornResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = UnprocessableFailureResponse(UnprocessableFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(
        mockOrnConnector
          .getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      ).thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.getOrn(fromDate, toDate))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(201, "{invalid json}")

      when(
        mockOrnConnector
          .getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      ).thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.getOrn(fromDate, toDate))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-201/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(
        mockOrnConnector
          .getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
            any[HeaderCarrier],
            any[ExecutionContext],
            ArgumentMatchers.eq(pillar2Id)
          )
      ).thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError] {
        await(service.getOrn(fromDate, toDate))
      }
    }
  }

  "amendOrn" - {
    "should return SuccessResponse for valid ORN request (200)" in {
      val successResponse = ORNSuccessResponse(ORNSuccess(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "123456789012345"))
      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpOk))

      val result = service.amendOrn(ornRequest).futureValue
      result mustBe successResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = UnprocessableFailureResponse(UnprocessableFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.amendOrn(ornRequest))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(200, "{invalid json}")

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.amendOrn(ornRequest))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-200/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError] {
        await(service.amendOrn(ornRequest))
      }
    }
  }
}
