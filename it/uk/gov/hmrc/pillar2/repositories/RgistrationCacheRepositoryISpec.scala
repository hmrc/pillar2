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
import uk.gov.hmrc.pillar2.repositories.RegistrationDataEntry.JsonDataEntry
import org.mongodb.scala.bson.BsonDocument
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.crypto.Sensitive
import play.api.libs.json.Reads
import uk.gov.hmrc.crypto.Crypted
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.Decrypter
import java.security.SecureRandom
import java.util.Base64
import play.api.Configuration
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

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


    "encrypt the json payload in the database" in {
      val crypto: Encrypter with Decrypter = {
        val aesKey = {
          val aesKey = new Array[Byte](32)
          new SecureRandom().nextBytes(aesKey)
          Base64.getEncoder.encodeToString(aesKey)
        }
        
        val config = Configuration("crypto.key" -> aesKey)
        SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", config.underlying)
      }
      
      def assertEncrypted[A: Reads](encryptedValue: String, expectedValue: A): Unit =
        Json.parse(crypto.decrypt(Crypted(encryptedValue)).value).as[A] shouldBe expectedValue

      val expecectedDataString = Sensitive.SensitiveString(userAnswersCache.data.toString())
      val mongoComponent = MongoComponent(mongoUri = "mongodb://localhost:27017/pillar2-test")
      (for {
        _ <- registrationCacheRepository.upsert(userAnswersCache.id, userAnswersCache.data)
        raw <- mongoComponent.database.getCollection[BsonDocument]("user-answers-records").find().headOption().map(_.value)
        _ = assertEncrypted(raw.get("data").asString.getValue, expecectedDataString.decryptedValue)
      } yield ()).futureValue
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
