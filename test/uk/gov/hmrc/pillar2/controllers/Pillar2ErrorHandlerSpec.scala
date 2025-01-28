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

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.pillar2.handlers.Pillar2ErrorHandler
import uk.gov.hmrc.pillar2.models.errors.{ObligationsAndSubmissionsError, Pillar2ApiError}

class Pillar2ErrorHandlerSpec extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  val classUnderTest = new Pillar2ErrorHandler
  val dummyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  test("Internal server error") {
    val response = classUnderTest.onServerError(dummyRequest, ObligationsAndSubmissionsError("500", "Internal server error"))
    status(response) mustEqual 500
    val result = contentAsJson(response).as[Pillar2ApiError]
    result.code mustEqual "500"
    result.message mustEqual "Internal server error"
  }

}
