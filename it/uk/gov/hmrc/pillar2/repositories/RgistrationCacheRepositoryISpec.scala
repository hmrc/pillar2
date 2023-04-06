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

package uk.gov.hmrc.pillar2.repositories


import play.api.libs.json.Json
import uk.gov.hmrc.pillar2.helpers.BaseISpec
import uk.gov.hmrc.pillar2.services.test.TestService
import uk.gov.hmrc.pillar2.repositories.RegistrationDataEntry.JsonDataEntry

class RgistrationCacheRepositoryISpec extends BaseISpec {
  val testService: TestService = app.injector.instanceOf[TestService]
  override def beforeEach(): Unit = {
    super.beforeEach()
    await(testService.clearAllData)
  }

  val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]

  "save" should {
    "successfully save data" in {
      registrationCacheRepository.upsert(userAnswersCache.id, userAnswersCache.data).futureValue
      val result  = registrationCacheRepository.get(userAnswersCache.id).futureValue.headOption.value
      val expectedRecord = JsonDataEntry(userAnswersCache.id, result, userAnswersCache.lastUpdated, userAnswersCache.expireAt)
      userAnswersCache shouldBe expectedRecord
    }

  }
  "get" should {
    "successfully get the record" in {
      registrationCacheRepository.upsert(userAnswersCache.id, userAnswersCache.data).futureValue
      val result  = registrationCacheRepository.get(userAnswersCache.id).futureValue.headOption.value
      val expectedRecord = JsonDataEntry(userAnswersCache.id, result, userAnswersCache.lastUpdated, userAnswersCache.expireAt)
      userAnswersCache shouldBe expectedRecord
    }

  }
  "remove" should {
    "successfully remove the record" in {
      registrationCacheRepository.upsert(userAnswersCache.id, userAnswersCache.data).futureValue
      val result  =registrationCacheRepository.remove(userAnswersCache.id).futureValue
      result shouldBe true

    }
  }
}
