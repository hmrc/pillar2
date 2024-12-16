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
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.hip._
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UktrSubmission

import java.time.ZonedDateTime
import scala.concurrent.Future

class UKTaxReturnServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service = new UKTaxReturnService(mockUKTaxReturnConnector)

  "UKTaxReturnService" - {
    "submitUKTaxReturn" - {
      "should return ApiSuccessResponse for successful submission (201)" in {
        val successResponse = ApiSuccessResponse(ApiSuccess(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "123456789012345", "12345678"))
        val httpResponse    = HttpResponse(201, Json.toJson(successResponse).toString())

        when(mockUKTaxReturnConnector.submitUKTaxReturn(any[UktrSubmission], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(httpResponse))

        forAll(arbitrary[UktrSubmission]) { submission =>
          val result = service.submitUKTaxReturn(submission, "XMPLR0000000012").futureValue
          result mustBe successResponse
        }
      }

      "should throw ValidationError for 422 response" in {
        val apiFailure   = ApiFailureResponse(ApiFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
        val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

        when(mockUKTaxReturnConnector.submitUKTaxReturn(any[UktrSubmission], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(httpResponse))

        forAll(arbitrary[UktrSubmission]) { submission =>
          val error = intercept[ETMPValidationError] {
            await(service.submitUKTaxReturn(submission, "XMPLR0000000012"))
          }
          error.code mustBe "422"
          error.message mustBe "Validation failed"
        }
      }

      "should throw InvalidJsonError for malformed success response" in {
        val httpResponse = HttpResponse(201, "{invalid json}")

        when(mockUKTaxReturnConnector.submitUKTaxReturn(any[UktrSubmission], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(httpResponse))

        forAll(arbitrary[UktrSubmission]) { submission =>
          val error = intercept[InvalidJsonError] {
            await(service.submitUKTaxReturn(submission, "XMPLR0000000012"))
          }
          error.code mustBe "002"
        }
      }

      "should throw ApiInternalServerError for non-201/422 responses" in {
        val httpResponse = HttpResponse(500, "{}")

        when(mockUKTaxReturnConnector.submitUKTaxReturn(any[UktrSubmission], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(httpResponse))

        forAll(arbitrary[UktrSubmission]) { submission =>
          intercept[ApiInternalServerError.type] {
            await(service.submitUKTaxReturn(submission, "XMPLR0000000012"))
          }
        }
      }
    }
  }
}
