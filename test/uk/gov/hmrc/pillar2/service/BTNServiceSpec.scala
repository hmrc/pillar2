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
import uk.gov.hmrc.pillar2.models.btn.BTNRequest
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}
import uk.gov.hmrc.pillar2.models.hip._

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class BTNServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service = new BTNService(mockBTNConnector)

  private val btnPayload =
    BTNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1)
    )

  "sendBtn" - {
    "should return ApiSuccessResponse for valid btnPayload (201)" in {
      when(mockBTNConnector.sendBtn(any[BTNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpCreated))

      val result = service.sendBtn(btnPayload).futureValue
      result mustBe successResponse
    }

    "should throw ValidationError for 422 response" in {
      val apiFailure   = ApiFailureResponse(ApiFailure(ZonedDateTime.parse("2024-03-14T09:26:17Z"), "422", "Validation failed"))
      val httpResponse = HttpResponse(422, Json.toJson(apiFailure).toString())

      when(mockBTNConnector.sendBtn(any[BTNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[ETMPValidationError] {
        await(service.sendBtn(btnPayload))
      }

      error.code mustBe "422"
      error.message mustBe "Validation failed"
    }

    "should throw InvalidJsonError for malformed success response" in {
      val httpResponse = HttpResponse(201, "{invalid json}")

      when(mockBTNConnector.sendBtn(any[BTNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      val error = intercept[InvalidJsonError] {
        await(service.sendBtn(btnPayload))
      }
      error.code mustBe "002"
    }

    "should throw ApiInternalServerError for non-201/422 responses" in {
      val httpResponse = HttpResponse(500, "{}")

      when(mockBTNConnector.sendBtn(any[BTNRequest])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(httpResponse))

      intercept[ApiInternalServerError.type] {
        await(service.sendBtn(btnPayload))
      }
    }
  }
}
