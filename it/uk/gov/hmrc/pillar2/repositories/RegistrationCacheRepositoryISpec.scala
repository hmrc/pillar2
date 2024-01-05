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

import uk.gov.hmrc.pillar2.helpers.BaseISpec
import uk.gov.hmrc.pillar2.service.test.TestService
import org.mongodb.scala.bson.BsonDocument
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import play.api.libs.json.Json

class RegistrationCacheRepositoryISpec extends BaseISpec {
  val testService: TestService = app.injector.instanceOf[TestService]
  override def beforeEach(): Unit = {
    super.beforeEach()
    await(testService.clearAllData)
  }

  val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]

  "save" should {
    "successfully save data" in {
      registrationCacheRepository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val result  = registrationCacheRepository.get(userAnswersCache.id).futureValue.headOption.value
      result.toString shouldBe userAnswersCache.data
    }

    "encrypt the json payload in the database" in {
      val crypto: Encrypter with Decrypter =  SymmetricCryptoFactory.aesGcmCrypto(registrationCacheCryptoKey)
      
      def assertEncrypted(encryptedValue: String, expectedValue: String): Unit = {
        crypto.decrypt(Crypted(encryptedValue)).value shouldBe expectedValue
      }

      val mongoComponent = MongoComponent(mongoUri = "mongodb://localhost:27017/pillar2-test")

      (for {
        _ <- registrationCacheRepository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data))
        raw <- mongoComponent.database.getCollection[BsonDocument]("user-answers-records").find().headOption().map(_.value)
        _ = assertEncrypted(raw.get("data").asString.getValue, userAnswersCache.data.toString())
      } yield ()).futureValue
    }
  }

  "get" should {
    "successfully get the record" in {
      registrationCacheRepository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val result  = registrationCacheRepository.get(userAnswersCache.id).futureValue.headOption.value
      result.toString shouldBe userAnswersCache.data
    }
  }

  "remove" should {
    "successfully remove the record" in {
      registrationCacheRepository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val result  =registrationCacheRepository.remove(userAnswersCache.id).futureValue
      result shouldBe true
    }
  }
}
