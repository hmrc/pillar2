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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.{BaseSpec, UKTaxReturnDataFixture}
import uk.gov.hmrc.pillar2.models.btn.{BTNRequest, BTNSuccess, BTNSuccessResponse}
import uk.gov.hmrc.pillar2.models.errors._
import uk.gov.hmrc.pillar2.service.BTNService

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class BTNControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with UKTaxReturnDataFixture {

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[BTNService].toInstance(mockBTNService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  private val btnPayload =
    BTNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1)
    )

  private val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.BTNController.submitBtn().url)
    .withHeaders("X-Pillar2-ID" -> pillar2Id)
    .withJsonBody(Json.toJson(btnPayload))

  "submitBtn" - {
    "should return Created with ApiSuccessResponse when submission is successful" in {
      val successResponse: BTNSuccessResponse = BTNSuccessResponse(
        BTNSuccess(processingDate = ZonedDateTime.parse("2024-03-14T09:26:17Z"))
      )

      when(mockBTNService.sendBtn(any[BTNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.successful(successResponse))

      val result = route(application, request).value

      status(result) mustEqual CREATED
      contentAsJson(result) mustEqual Json.toJson(successResponse.success)
    }

    "should return MissingHeaderError when X-Pillar2-Id header is missing" in {

      val headlessRequest: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.BTNController.submitBtn().url)
        .withJsonBody(Json.toJson(btnPayload))

      val result = intercept[MissingHeaderError](await(route(application, headlessRequest).value))
      result mustEqual MissingHeaderError("X-Pillar2-Id")
    }

    "should return AuthorizationError when authentication fails" in {
      val unauthorizedApp = new GuiceApplicationBuilder()
        .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
        .overrides(bind[BTNService].toInstance(mockBTNService))
        .build()

      val result = intercept[AuthorizationError.type](await(route(unauthorizedApp, request).value))
      result mustEqual AuthorizationError
    }

    "should handle ValidationError from service" in {
      when(mockBTNService.sendBtn(any[BTNRequest])(using any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(ETMPValidationError("422", "Validation failed")))

      val result = intercept[ETMPValidationError](await(route(application, request).value))
      result mustEqual ETMPValidationError("422", "Validation failed")
    }

    "should handle InvalidJsonError from service" in {
      when(mockBTNService.sendBtn(any[BTNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.failed(InvalidJsonError("Invalid JSON")))

      val result = intercept[InvalidJsonError](await(route(application, request).value))
      result mustEqual InvalidJsonError("Invalid JSON")
    }

    "should handle ApiInternalServerError from service" in {
      when(mockBTNService.sendBtn(any[BTNRequest])(using any[HeaderCarrier], any[String])).thenReturn(Future.failed(ApiInternalServerError))

      val result = intercept[ApiInternalServerError.type](await(route(application, request).value))
      result mustEqual ApiInternalServerError
    }
  }
}
