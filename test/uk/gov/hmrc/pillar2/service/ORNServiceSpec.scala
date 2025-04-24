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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.hip._
import uk.gov.hmrc.pillar2.models.orn.{ORNRequest, ORNSuccess, ORNSuccessResponse}

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class ORNServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service = new ORNService(mockOrnConnector)

  private val ornPayload =
    ORNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1),
      filedDateGIR = LocalDate.now().plusYears(1),
      countryGIR = "US",
      reportingEntityName = "Newco PLC",
      TIN = "US12345678",
      issuingCountryTIN = "US"
    )

  "submitOrn" - {
    "should return SuccessResponse for valid ornPayload (201)" in {
      val successResponse = ORNSuccessResponse(ORNSuccess(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "123456789012345"))
      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpCreated))

      val result = service.submitOrn(ornPayload).futureValue
      result mustBe successResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = ApiFailureResponse(ApiFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.submitOrn(ornPayload))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(201, "{invalid json}")

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.submitOrn(ornPayload))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-201/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(mockOrnConnector.submitOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError.type] {
        await(service.submitOrn(ornPayload))
      }
    }
  }

  "amendOrn" - {
    "should return SuccessResponse for valid ornPayload (200)" in {
      val successResponse = ORNSuccessResponse(ORNSuccess(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "123456789012345"))
      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpOk))

      val result = service.amendOrn(ornPayload).futureValue
      result mustBe successResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = ApiFailureResponse(ApiFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.amendOrn(ornPayload))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(200, "{invalid json}")

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.amendOrn(ornPayload))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-200/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(mockOrnConnector.amendOrn(any[ORNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError.type] {
        await(service.amendOrn(ornPayload))
      }
    }
  }
}
