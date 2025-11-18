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

package uk.gov.hmrc.pillar2.handlers

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.pillar2.models.errors._

class Pillar2ErrorHandlerSpec extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  val classUnderTest = new Pillar2ErrorHandler
  val dummyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  test("client errors should be returned") {
    val validStatus = Gen.choose(400, 499)
    val messageGen  = Gen.alphaStr
    forAll(validStatus, messageGen) { (statusCode, message) =>
      val result = classUnderTest.onClientError(dummyRequest, statusCode, message)
      status(result) mustEqual statusCode
      val response = contentAsJson(result).as[Pillar2ApiError]
      response.message mustEqual message
      response.code mustEqual statusCode.toString
    }
  }

  test("Catch-all error response") {
    val response = classUnderTest.onServerError(dummyRequest, new RuntimeException("Generic Error"))
    status(response) mustEqual 500
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "003"
    result.message mustEqual "Internal server error"
  }

  test("MissingHeaderError error response") {
    val response = classUnderTest.onServerError(dummyRequest, MissingHeaderError("test"))
    status(response) mustEqual 400
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "001"
    result.message mustEqual "Missing test header"
  }

  test("ETMPValidationError error response") {
    val response = classUnderTest.onServerError(dummyRequest, ETMPValidationError("test code", "test ETMP validation error"))
    status(response) mustEqual 422
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "test code"
    result.message mustEqual "test ETMP validation error"
  }

  test("InvalidJsonError error response") {
    val response = classUnderTest.onServerError(dummyRequest, InvalidJsonError("test decode error"))
    status(response) mustEqual 500
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "002"
    result.message mustEqual "Invalid JSON payload: test decode error"
  }

  test("ApiInternalServerError error response") {
    val response = classUnderTest.onServerError(dummyRequest, ApiInternalServerError)
    status(response) mustEqual 500
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "003"
    result.message mustEqual "Internal server error"
  }

  test("AuthorizationError error response") {
    val response = classUnderTest.onServerError(dummyRequest, AuthorizationError)
    status(response) mustEqual 401
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "401"
    result.message mustEqual "Not Authorized"
  }

}
