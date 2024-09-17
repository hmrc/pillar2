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

///*
// * Copyright 2024 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.pillar2.controllers
//
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.{reset, when}
//import org.scalacheck.Gen
//import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.json.Json
//import play.api.mvc.Result
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{GET, contentAsJson, route, status, writeableOf_AnyContentAsEmpty}
//import play.api.{Application, Configuration}
//import uk.gov.hmrc.auth.core.AuthConnector
//import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
//import uk.gov.hmrc.pillar2.generators.Generators
//import uk.gov.hmrc.pillar2.helpers.BaseSpec
//import uk.gov.hmrc.pillar2.models.FinancialDataError
//import uk.gov.hmrc.pillar2.models.financial.{FinancialHistory, TransactionHistory}
//import uk.gov.hmrc.pillar2.service.FinancialService
//
//import java.time.LocalDate
//import scala.concurrent.Future
//class FinancialDataControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
//  val application: Application = new GuiceApplicationBuilder()
//    .configure(
//      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
//    )
//    .overrides(
//      bind[AuthConnector].toInstance(mockAuthConnector),
//      bind[AuthAction].to[FakeAuthAction],
//      bind[FinancialService].to(mockFinancialService)
//    )
//    .build()
//
//  override def afterEach(): Unit = {
//    reset(mockFinancialService, mockAuthConnector)
//    super.afterEach()
//  }
//
//  val paymentHistoryResult: String => TransactionHistory = (plrReference: String) =>
//    TransactionHistory(
//      plrReference,
//      List(
//        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 100.0, 0.00),
//        FinancialHistory(LocalDate.now.plusDays(2), "Repayment", 0.0, 100.0)
//      )
//    )
//
//  "FinancialDataController" - {
//
//    "should return OK with payment history when payment history is found for plrReference" in {
//      forAll(plrReferenceGen) { plrReference =>
//        when(mockFinancialService.getTransactionHistory(any())(any())).thenReturn(Future successful Right(paymentHistoryResult(plrReference)))
//
//        val request = FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference).url)
//        val result: Future[Result] = route(application, request).value
//
//        status(result) mustBe 200
//        contentAsJson(result) mustBe Json.toJson(paymentHistoryResult(plrReference))
//      }
//    }
//
//    "should return Not found when api returns not found" in {
//      val dataError = FinancialDataError("NOT_FOUND", "Some reason")
//      when(mockFinancialService.getTransactionHistory(any())(any())).thenReturn(Future successful Left(dataError))
//
//      val request = FakeRequest(GET, routes.FinancialDataController.getTransactionHistory("XMPLR0123456789").url)
//      val result: Future[Result] = route(application, request).value
//
//      status(result) mustBe 404
//      contentAsJson(result) mustBe Json.toJson(dataError)
//    }
//
//    "should return failed dependency when api returns server error or service unavailable" in {
//      forAll(plrReferenceGen, Gen.oneOf(Seq("SERVER_ERROR", "SERVICE_UNAVAILABLE"))) { (plrReference, errorCode) =>
//        val dataError = FinancialDataError(errorCode, "Some reason")
//
//        when(mockFinancialService.getTransactionHistory(any())(any())).thenReturn(Future successful Left(dataError))
//
//        val request = FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference).url)
//        val result: Future[Result] = route(application, request).value
//
//        status(result) mustBe 424
//        contentAsJson(result) mustBe Json.toJson(dataError)
//      }
//    }
//
//    "should return bad request for all other errors" in {
//
//      forAll(plrReferenceGen, stringsExceptSpecificValues(Seq("NOT_FOUND", "SERVER_ERROR", "SERVICE_UNAVAILABLE"))) { (plrReference, errorCode) =>
//        val dataError = FinancialDataError(errorCode, "Some reason")
//
//        when(mockFinancialService.getTransactionHistory(any())(any())).thenReturn(Future successful Left(dataError))
//
//        val request = FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference).url)
//        val result: Future[Result] = route(application, request).value
//
//        status(result) mustBe 400
//        contentAsJson(result) mustBe Json.toJson(dataError)
//      }
//    }
//  }
//
//}

package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, route, status, writeableOf_AnyContentAsEmpty}
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.FinancialDataError
import uk.gov.hmrc.pillar2.models.financial.{FinancialHistory, TransactionHistory}
import uk.gov.hmrc.pillar2.service.FinancialService

import java.time.LocalDate
import scala.concurrent.Future

class FinancialDataControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction],
      bind[FinancialService].to(mockFinancialService)
    )
    .build()

  override def afterEach(): Unit = {
    reset(mockFinancialService, mockAuthConnector)
    super.afterEach()
  }

  val paymentHistoryResult: String => TransactionHistory = (plrReference: String) =>
    TransactionHistory(
      plrReference,
      List(
        FinancialHistory(LocalDate.now.plusDays(1), "Payment", 100.0, 0.00),
        FinancialHistory(LocalDate.now.plusDays(2), "Repayment", 0.0, 100.0)
      )
    )

  val startDate: String = LocalDate.now().toString // Explicit type ascription added
  val endDate:   String = LocalDate.now().plusYears(1).toString // Explicit type ascription added

  "FinancialDataController" - {

    "should return OK with payment history when payment history is found for plrReference" in {
      forAll(plrReferenceGen) { plrReference =>
        when(mockFinancialService.getTransactionHistory(any(), any(), any())(any()))
          .thenReturn(Future successful Right(paymentHistoryResult(plrReference)))

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference, startDate, endDate).url)
        val result: Future[Result] = route(application, request).value

        status(result) mustBe 200
        contentAsJson(result) mustBe Json.toJson(paymentHistoryResult(plrReference))
      }
    }

    "should return Not found when api returns not found" in {
      val dataError = FinancialDataError("NOT_FOUND", "Some reason")
      when(mockFinancialService.getTransactionHistory(any(), any(), any())(any())).thenReturn(Future successful Left(dataError))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.FinancialDataController.getTransactionHistory("XMPLR0123456789", startDate, endDate).url)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe 404
      contentAsJson(result) mustBe Json.toJson(dataError)
    }

    "should return failed dependency when api returns server error or service unavailable" in {
      forAll(plrReferenceGen, Gen.oneOf(Seq("SERVER_ERROR", "SERVICE_UNAVAILABLE"))) { (plrReference, errorCode) =>
        val dataError = FinancialDataError(errorCode, "Some reason")

        when(mockFinancialService.getTransactionHistory(any(), any(), any())(any())).thenReturn(Future successful Left(dataError))

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference, startDate, endDate).url)
        val result: Future[Result] = route(application, request).value

        status(result) mustBe 424
        contentAsJson(result) mustBe Json.toJson(dataError)
      }
    }

    "should return bad request for all other errors" in {

      forAll(plrReferenceGen, stringsExceptSpecificValues(Seq("NOT_FOUND", "SERVER_ERROR", "SERVICE_UNAVAILABLE"))) { (plrReference, errorCode) =>
        val dataError = FinancialDataError(errorCode, "Some reason")

        when(mockFinancialService.getTransactionHistory(any(), any(), any())(any())).thenReturn(Future successful Left(dataError))

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.FinancialDataController.getTransactionHistory(plrReference, startDate, endDate).url)
        val result: Future[Result] = route(application, request).value

        status(result) mustBe 400
        contentAsJson(result) mustBe Json.toJson(dataError)
      }
    }
  }
}
