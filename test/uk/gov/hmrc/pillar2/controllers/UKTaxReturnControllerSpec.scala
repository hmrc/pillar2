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

package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hip.{ApiSuccess, ApiSuccessResponse, ErrorSummary}
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UktrSubmission
import uk.gov.hmrc.pillar2.service.UKTaxReturnService
import play.api.mvc.Results._

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future

class UKTaxReturnControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[UKTaxReturnService].toInstance(mockUKTaxReturnService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "UKTaxReturnController" - {
    "submitUKTaxReturn" - {
      "should return OK with success response when service returns CREATED (201)" in {
        forAll(arbitrary[UktrSubmission]) { submission =>
        val response =  ApiSuccessResponse(ApiSuccess(LocalDateTime.now(ZoneId.of("UTC")), "123456789012345", "dummyChargeReference"))

          when(mockUKTaxReturnService.submitUKTaxReturn(eqTo(submission), any[String])(any[HeaderCarrier]))
            .thenReturn(Future.successful(response))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> "XMPLR0000000012")
            .withJsonBody(Json.toJson(submission))

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsJson(result).as[ApiSuccessResponse] mustEqual response
        }
      }

      "should return UNPROCESSABLE_ENTITY when service returns 422" in {
        forAll(arbitraryUktrSubmission.arbitrary) { submission =>
          val errorResponse = Json.obj(
            "errors" -> Json.obj(
              "code" -> "003",
              "text" -> "Request could not be processed"
            )
          )

          when(mockUKTaxReturnService.submitUKTaxReturn(eqTo(submission), any[String])(any[HeaderCarrier]))
            .thenThrow()

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> "XMPLR0000000012")
            .withJsonBody(Json.toJson(submission))

          val result = route(application, request).value

          status(result) mustBe UNPROCESSABLE_ENTITY
          contentAsJson(result) mustBe Json.toJson(ErrorSummary("003", "Request could not be processed"))
        }
      }

      "should return BAD_REQUEST when X-Pillar2-Id header is missing" in {
        forAll(arbitraryUktrSubmission.arbitrary) { submission =>
          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withJsonBody(Json.toJson(submission))

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe Json.toJson(
            ErrorSummary(
              "400",
              "Missing X-Pillar2-Id header"
            )
          )
        }
      }
    }
  }
}
