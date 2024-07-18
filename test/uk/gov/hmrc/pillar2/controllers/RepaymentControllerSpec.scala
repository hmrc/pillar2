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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.pillar2.service.{RepaymentService, SubscriptionService}

import scala.concurrent.Future

class RepaymentControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[RepaymentService].toInstance(mockRepaymentService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  val service =
    new SubscriptionService(
      mockedCache,
      mockSubscriptionConnector,
      mockAuditService
    )

  override def afterEach(): Unit = {
    reset(mockRegistrationCacheRepository, mockAuthConnector, mockSubscriptionService)
    super.afterEach()
  }

  "RepaymentController" - {
    "should return BAD_REQUEST when  repayment parameter is invalid" in {

      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(
          POST,
          routes.RepaymentController.repaymentsSendRequest.url
        )
          .withJsonBody(Json.parse("""{"value": "field"}"""))

      val result: Future[Result] = route(application, request).value
      status(result) mustEqual BAD_REQUEST
    }

    "should return CREATED when a valid json is passed and the call to ETMP is successfull" in {
      forAll(arbitraryRepaymentPayload.arbitrary) { repaymentPayload =>
        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(
            POST,
            routes.RepaymentController.repaymentsSendRequest.url
          )
            .withJsonBody(Json.toJson(repaymentPayload))
        when(mockRepaymentService.sendRepaymentsData(any[RepaymentRequestDetail])(any())).thenReturn(Future.successful(Done))
        val result: Future[Result] = route(application, request).value
        status(result) mustEqual CREATED
      }
    }
  }
}
