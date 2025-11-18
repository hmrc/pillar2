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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.{BaseSpec, ORNDataFixture}
import uk.gov.hmrc.pillar2.models.errors._
import uk.gov.hmrc.pillar2.models.orn.{ORNRequest, ORNSuccess, ORNSuccessResponse}
import uk.gov.hmrc.pillar2.service.ORNService

import java.time.ZonedDateTime
import scala.concurrent.Future

class ORNControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ORNDataFixture {

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[ORNService].toInstance(mockOrnService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  private val submitOrnRequest: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.ORNController.submitOrn().url)
    .withHeaders("X-Pillar2-ID" -> pillar2Id)
    .withJsonBody(Json.toJson(ornRequest))

  private val amendOrnRequest: FakeRequest[AnyContentAsJson] = FakeRequest(PUT, routes.ORNController.amendOrn().url)
    .withHeaders("X-Pillar2-ID" -> pillar2Id)
    .withJsonBody(Json.toJson(ornRequest))

  private val getOrnRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.ORNController.getOrn(fromDate.toString, toDate.toString).url)
      .withHeaders("X-Pillar2-ID" -> pillar2Id)

  "submitOrn" - {
    "should return Created with OrnSuccessResponse when submission is successful" in {
      val successResponse: ORNSuccessResponse = ORNSuccessResponse(
        ORNSuccess(processingDate = ZonedDateTime.parse("2024-03-14T09:26:17Z"), formBundleNumber = "123456789012345")
      )

      when(mockOrnService.submitOrn(any[ORNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.successful(successResponse))

      val result = route(application, submitOrnRequest).value

      status(result) mustEqual CREATED
      contentAsJson(result) mustEqual Json.toJson(successResponse.success)
    }

    "should return MissingHeaderError when X-Pillar2-Id header is missing" in {

      val headlessRequest: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.ORNController.submitOrn().url)
        .withJsonBody(Json.toJson(ornRequest))

      val result = intercept[MissingHeaderError](await(route(application, headlessRequest).value))
      result mustEqual MissingHeaderError("X-Pillar2-Id")
    }

    "should return AuthorizationError when authentication fails" in {
      val unauthorizedApp = new GuiceApplicationBuilder()
        .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
        .overrides(bind[ORNService].toInstance(mockOrnService))
        .build()

      val result = intercept[AuthorizationError.type](await(route(unauthorizedApp, submitOrnRequest).value))
      result mustEqual AuthorizationError
    }

    "should handle ValidationError from service" in {
      when(mockOrnService.submitOrn(any[ORNRequest])(using any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(ETMPValidationError("422", "Validation failed")))

      val result = intercept[ETMPValidationError](await(route(application, submitOrnRequest).value))
      result mustEqual ETMPValidationError("422", "Validation failed")
    }

    "should handle InvalidJsonError from service" in {
      when(mockOrnService.submitOrn(any[ORNRequest])(using any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(InvalidJsonError("Invalid JSON")))

      val result = intercept[InvalidJsonError](await(route(application, submitOrnRequest).value))
      result mustEqual InvalidJsonError("Invalid JSON")
    }

    "should handle ApiInternalServerError from service" in {
      when(mockOrnService.submitOrn(any[ORNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.failed(ApiInternalServerError))

      val result = intercept[ApiInternalServerError.type](await(route(application, submitOrnRequest).value))
      result mustEqual ApiInternalServerError
    }
  }

  "amendOrn" - {
    "should return Accepted with ORNSuccessResponse when amendment is successful" in {
      val successResponse: ORNSuccessResponse = ORNSuccessResponse(
        ORNSuccess(processingDate = ZonedDateTime.parse("2024-03-14T09:26:17Z"), formBundleNumber = "123456789012345")
      )

      when(mockOrnService.amendOrn(any[ORNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.successful(successResponse))

      val result = route(application, amendOrnRequest).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(successResponse.success)
    }

    "should return MissingHeaderError when X-Pillar2-Id header is missing" in {

      val headlessRequest: FakeRequest[AnyContentAsJson] = FakeRequest(PUT, routes.ORNController.amendOrn().url)
        .withJsonBody(Json.toJson(ornRequest))

      val result = intercept[MissingHeaderError](await(route(application, headlessRequest).value))
      result mustEqual MissingHeaderError("X-Pillar2-Id")
    }

    "should handle ValidationError from service" in {
      when(mockOrnService.amendOrn(any[ORNRequest])(using any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(ETMPValidationError("422", "Validation failed")))

      val result = intercept[ETMPValidationError](await(route(application, amendOrnRequest).value))
      result mustEqual ETMPValidationError("422", "Validation failed")
    }

    "should handle InvalidJsonError from service" in {
      when(mockOrnService.amendOrn(any[ORNRequest])(using any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(InvalidJsonError("Invalid JSON")))

      val result = intercept[InvalidJsonError](await(route(application, amendOrnRequest).value))
      result mustEqual InvalidJsonError("Invalid JSON")
    }

    "should handle ApiInternalServerError from service" in {
      when(mockOrnService.amendOrn(any[ORNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.failed(ApiInternalServerError))

      val result = intercept[ApiInternalServerError.type](await(route(application, amendOrnRequest).value))
      result mustEqual ApiInternalServerError
    }
  }

  "getOrn" - {
    "should return 200 with getOrnSuccessResponse when getOrn is successful" in {
      when(
        mockOrnService.getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      ).thenReturn(Future.successful(ornResponse))

      val result = route(application, getOrnRequest).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ornResponse.success)

    }

    "should return MissingHeaderError when X-Pillar2-Id header is missing" in {

      val headlessRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.ORNController.getOrn(fromDate.toString, toDate.toString).url)

      val result = intercept[MissingHeaderError](await(route(application, headlessRequest).value))
      result mustEqual MissingHeaderError("X-Pillar2-Id")
    }

    "should return AuthorizationError when authentication fails" in {
      val unauthorizedApp = new GuiceApplicationBuilder()
        .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
        .overrides(bind[ORNService].toInstance(mockOrnService))
        .build()

      val result = intercept[AuthorizationError.type](await(route(unauthorizedApp, getOrnRequest).value))
      result mustEqual AuthorizationError
    }

    "should handle ValidationError from service" in {
      when(
        mockOrnService.getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      ).thenReturn(Future.failed(ETMPValidationError("422", "Validation failed")))

      val result = intercept[ETMPValidationError](await(route(application, getOrnRequest).value))
      result mustEqual ETMPValidationError("422", "Validation failed")
    }

    "should handle InvalidJsonError from service" in {
      when(
        mockOrnService.getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      ).thenReturn(Future.failed(InvalidJsonError("Invalid JSON")))

      val result = intercept[InvalidJsonError](await(route(application, getOrnRequest).value))
      result mustEqual InvalidJsonError("Invalid JSON")
    }

    "should handle ApiInternalServerError from service" in {
      when(
        mockOrnService.getOrn(ArgumentMatchers.eq(fromDate), ArgumentMatchers.eq(toDate))(
          using
          any[HeaderCarrier],
          ArgumentMatchers.eq(pillar2Id)
        )
      ).thenReturn(Future.failed(ApiInternalServerError))

      val result = intercept[ApiInternalServerError.type](await(route(application, getOrnRequest).value))
      result mustEqual ApiInternalServerError
    }
  }
}
