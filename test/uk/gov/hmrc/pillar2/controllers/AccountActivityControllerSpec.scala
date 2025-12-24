/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Result
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction, Pillar2HeaderAction}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.accountactivity.AccountActivitySuccess
import uk.gov.hmrc.pillar2.models.errors.*
import uk.gov.hmrc.pillar2.service.AccountActivityService

import java.time.LocalDate
import scala.concurrent.Future

class AccountActivityControllerSpec extends BaseSpec {

  val sampleAccountActivityResponse: JsValue = Json.parse(getClass.getResourceAsStream("/data/sample-account-activity.json"))

  val today:    LocalDate = LocalDate.now()
  val aYearAgo: LocalDate = today.minusYears(1)

  trait AccountActivityControllerTestCase(serviceResponse: Future[AccountActivitySuccess]) {
    val mockService: AccountActivityService = {
      val service = mock[AccountActivityService]
      when(service.getAccountActivity(any(), any())(using any())).thenReturn(serviceResponse)
      service
    }

    val controller: AccountActivityController = AccountActivityController(
      mockService,
      FakeAuthAction(app.injector.instanceOf[BodyParsers.Default]),
      app.injector.instanceOf[Pillar2HeaderAction],
      app.injector.instanceOf[ControllerComponents]
    )

    val headerlessRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      routes.AccountActivityController.getAccountActivity(aYearAgo.toString, today.toString)
    )

    val requestWithPillarId: FakeRequest[AnyContentAsEmpty.type] = headerlessRequest.withHeaders("X-Pillar2-Id" -> pillar2Id)
  }

  "getAccountActivity" - {
    "returns a success response matching ETMP's, with the 'success' field removed" in new AccountActivityControllerTestCase(
      serviceResponse = Future.successful(sampleAccountActivityResponse.as[AccountActivitySuccess])
    ) {
      val result: Future[Result] = controller.getAccountActivity(dateFrom = aYearAgo.toString, dateTo = today.toString)(requestWithPillarId)

      status(result) mustBe OK
      contentAsJson(result) mustBe (sampleAccountActivityResponse \ "success").get
    }

    "returns a 400 when passed malformatted dates" in new AccountActivityControllerTestCase(
      serviceResponse = Future.successful(sampleAccountActivityResponse.as[AccountActivitySuccess])
    ) {
      val badFrom: Future[Result] = controller.getAccountActivity(dateFrom = "not-a-date", dateTo = today.toString)(requestWithPillarId)
      val badTo:   Future[Result] = controller.getAccountActivity(dateFrom = aYearAgo.toString, dateTo = "not-a-date")(requestWithPillarId)

      status(badFrom) mustBe BAD_REQUEST
      status(badTo) mustBe BAD_REQUEST
      contentAsJson(badFrom) mustBe Json.toJson(Pillar2ApiError(code = "400", message = "Invalid date format."))
      contentAsJson(badTo) mustBe Json.toJson(Pillar2ApiError(code = "400", message = "Invalid date format."))
    }

    "returns MissingHeaderError when X-Pillar2-Id header is missing" in new AccountActivityControllerTestCase(
      serviceResponse = Future.successful(sampleAccountActivityResponse.as[AccountActivitySuccess])
    ) {
      val result: MissingHeaderError = intercept[MissingHeaderError] {
        throw controller.getAccountActivity(aYearAgo.toString, today.toString)(headerlessRequest).failed.futureValue
      }
      result mustEqual MissingHeaderError("X-Pillar2-Id")
    }

    "returns AuthorizationError when authentication fails" in new AccountActivityControllerTestCase(
      serviceResponse = Future.successful(sampleAccountActivityResponse.as[AccountActivitySuccess])
    ) {
      val realAuthController: AccountActivityController = AccountActivityController(
        mockService,
        app.injector.instanceOf[AuthAction],
        app.injector.instanceOf[Pillar2HeaderAction],
        app.injector.instanceOf[ControllerComponents]
      )

      val result: AuthorizationError.type = intercept[AuthorizationError.type] {
        throw realAuthController.getAccountActivity(aYearAgo.toString, today.toString)(headerlessRequest).failed.futureValue
      }
      result mustEqual AuthorizationError
    }

    "handles ValidationError from service" in new AccountActivityControllerTestCase(
      serviceResponse = Future.failed(ETMPValidationError("422", "Validation failed"))
    ) {
      val result: ETMPValidationError = intercept[ETMPValidationError] {
        throw controller.getAccountActivity(aYearAgo.toString, today.toString)(requestWithPillarId).failed.futureValue
      }
      result mustEqual ETMPValidationError("422", "Validation failed")
    }

    "handles InvalidJson from service" in new AccountActivityControllerTestCase(
      serviceResponse = Future.failed(InvalidJsonError("invalid-json"))
    ) {
      val result: InvalidJsonError = intercept[InvalidJsonError] {
        throw controller.getAccountActivity(aYearAgo.toString, today.toString)(requestWithPillarId).failed.futureValue
      }
      result mustEqual InvalidJsonError("invalid-json")
    }

    "handles ApiInternalServerError from service" in new AccountActivityControllerTestCase(
      serviceResponse = Future.failed(ApiInternalServerError)
    ) {
      val result: ApiInternalServerError.type = intercept[ApiInternalServerError.type] {
        throw controller.getAccountActivity(aYearAgo.toString, today.toString)(requestWithPillarId).failed.futureValue
      }
      result mustEqual ApiInternalServerError
    }
  }

}
