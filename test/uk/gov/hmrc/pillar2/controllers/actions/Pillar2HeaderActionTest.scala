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

package uk.gov.hmrc.pillar2.controllers.actions

import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldEqual
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.pillar2.models.errors.MissingHeaderError

import scala.concurrent.ExecutionContext.Implicits.*

class Pillar2HeaderActionTest extends AnyFunSuite with EitherValues {

  val classUnderTest = new Pillar2HeaderAction()

  test("X-Pillar2-ID header exists") {
    val request = FakeRequest().withHeaders("X-Pillar2-ID" -> "XXXXXXXXXXXXXXXXX")

    val result = await(classUnderTest.transform(request))

    result.pillar2Id shouldEqual "XXXXXXXXXXXXXXXXX"
  }

  test("X-Pillar2-ID header does not exist") {
    val request = FakeRequest() // with no headers

    val result = intercept[MissingHeaderError](await(classUnderTest.transform(request)))

    result.code shouldEqual "001"
    result.message shouldEqual "Missing X-Pillar2-Id header"
  }

}
