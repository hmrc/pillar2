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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
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
import uk.gov.hmrc.pillar2.models.errors._
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UKTRSubmission
import uk.gov.hmrc.pillar2.service.UKTaxReturnService

import scala.concurrent.Future

class UKTaxReturnControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[UKTaxReturnService].toInstance(mockUKTaxReturnService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "UKTaxReturnController" - {
    "submitUKTaxReturn" - {
      "forward the X-Pillar2-Id header" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          val captor = ArgumentCaptor.forClass(classOf[HeaderCarrier])
          when(mockUKTaxReturnService.submitUKTaxReturn(any[UKTRSubmission])(captor.capture()))
            .thenReturn(Future.successful(successResponse))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = route(application, request).value

          status(result) mustEqual CREATED
          contentAsJson(result) mustEqual Json.toJson(successResponse.success)
          captor.getValue.extraHeaders.map(_._1) must contain("X-Pillar2-Id")
          captor.getValue.extraHeaders.map(_._2).head mustEqual pillar2Id
        }
      }

      "should return Created with ApiSuccessResponse when submission is successful" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          when(mockUKTaxReturnService.submitUKTaxReturn(any[UKTRSubmission])(any[HeaderCarrier]))
            .thenReturn(Future.successful(successResponse))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = route(application, request).value

          status(result) mustEqual CREATED
          contentAsJson(result) mustEqual Json.toJson(successResponse.success)
        }
      }

      "should return MissingHeaderError when X-Pillar2-Id header is missing" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withJsonBody(Json.toJson(submission))

          val result = intercept[MissingHeaderError](await(route(application, request).value))
          result mustEqual MissingHeaderError("X-Pillar2-Id")
        }
      }

      // Maybe we should update the auth action to make use of the error handler?

      "should return UNAUTHORIZED when authentication fails" in {
        val unauthorizedApp = new GuiceApplicationBuilder()
          .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
          .overrides(
            bind[UKTaxReturnService].toInstance(mockUKTaxReturnService)
          )
          .build()

        forAll(arbitrary[UKTRSubmission]) { submission =>
          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = route(unauthorizedApp, request).value
          status(result) mustEqual UNAUTHORIZED
        }
      }

      //Everything after this is perhaps useless

      "should handle ValidationError from service" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          when(mockUKTaxReturnService.submitUKTaxReturn(any[UKTRSubmission])(any[HeaderCarrier]))
            .thenReturn(Future.failed(ETMPValidationError("422", "Validation failed")))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = intercept[ETMPValidationError](await(route(application, request).value))
          result mustEqual ETMPValidationError("422", "Validation failed")
        }
      }

      "should handle InvalidJsonError from service" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          when(mockUKTaxReturnService.submitUKTaxReturn(any[UKTRSubmission])(any[HeaderCarrier]))
            .thenReturn(Future.failed(InvalidJsonError("Invalid JSON")))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = intercept[InvalidJsonError](await(route(application, request).value))
          result mustEqual InvalidJsonError("Invalid JSON")
        }
      }

      "should handle ApiInternalServerError from service" in {
        forAll(arbitrary[UKTRSubmission]) { submission =>
          when(mockUKTaxReturnService.submitUKTaxReturn(any[UKTRSubmission])(any[HeaderCarrier]))
            .thenReturn(Future.failed(ApiInternalServerError))

          val request = FakeRequest(POST, routes.UKTaxReturnController.submitUKTaxReturn().url)
            .withHeaders("X-Pillar2-Id" -> pillar2Id)
            .withJsonBody(Json.toJson(submission))

          val result = intercept[ApiInternalServerError.type](await(route(application, request).value))
          result mustEqual ApiInternalServerError
        }
      }
    }
  }
}
