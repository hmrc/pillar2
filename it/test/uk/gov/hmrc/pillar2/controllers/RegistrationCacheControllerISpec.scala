/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.OptionValues
import play.api.libs.json.Json
import uk.gov.hmrc.pillar2.helpers.{BaseISpec, WireMockConfig, WireMockSupport}
import uk.gov.hmrc.pillar2.repositories.{RegistrationCacheRepository, RegistrationDataEntry}
import uk.gov.hmrc.pillar2.service.test.TestService

import java.time.Instant

class RegistrationCacheControllerISpec extends BaseISpec with WireMockSupport with WireMockConfig with OptionValues {

  val testService: TestService = app.injector.instanceOf[TestService]
  override def beforeEach(): Unit = {
    super.beforeEach()
    await(testService.clearAllData)
  }

  val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]
  val controller:                  RegistrationCacheController = app.injector.instanceOf[RegistrationCacheController]
  private val userAnswersCache =
    RegistrationDataEntry(
      "id",
      Json.toJson(("foo" -> "bar", "name" -> "steve", "address" -> "address1")).toString(),
      Instant.now()
    )

  "save" should {
    "successfully save data" in {
      stubAuthenticate()
      val example = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
      val result = callRoute(
        fakeRequest(routes.RegistrationCacheController.save(userAnswersCache.id))
          .withHeaders(contentType)
          .withBody(example)
      )
      status(result) shouldBe 200
      val expectedResult = registrationCacheRepository.get(userAnswersCache.id).futureValue.value
      example shouldBe expectedResult
    }

  }

  "get" should {
    "successfully get the record" in {
      stubAuthenticate()
      val example = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
      val result = callRoute(
        fakeRequest(routes.RegistrationCacheController.save(userAnswersCache.id))
          .withHeaders(contentType)
          .withBody(example)
      )
      status(result) shouldBe 200
      val expectedResult = registrationCacheRepository.get(userAnswersCache.id).futureValue.value
      example shouldBe expectedResult
    }

  }
  "remove" should {
    "successfully remove the record" in {
      stubAuthenticate()
      val example = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
      val result = callRoute(
        fakeRequest(routes.RegistrationCacheController.save(userAnswersCache.id))
          .withHeaders(contentType)
          .withBody(example)
      )
      status(result) shouldBe 200
      val expectedResult = registrationCacheRepository.remove(userAnswersCache.id).futureValue
      expectedResult shouldBe true

    }
  }

}
