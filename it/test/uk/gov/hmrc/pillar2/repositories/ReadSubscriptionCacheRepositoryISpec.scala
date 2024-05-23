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

import org.mongodb.scala.bson.BsonDocument
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class ReadSubscriptionCacheRepositoryISpec
    extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[RegistrationDataEntry]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val app = GuiceApplicationBuilder()
    .configure(
      "encryptionToggle" -> "true"
    )
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  override protected val repository: ReadSubscriptionCacheRepository =
    app.injector.instanceOf[ReadSubscriptionCacheRepository]

  private val cryptoKey = app.configuration.get[String]("registrationCache.key")

  private val userAnswersCache =
    RegistrationDataEntry(
      "id",
      Json.toJson("foo" -> "bar", "name" -> "steve", "address" -> "address1").toString(),
      Instant.now()
    )

  "save" should {
    "successfully save data" in {
      repository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val result = repository.get(userAnswersCache.id).futureValue.value
      result.toString mustBe userAnswersCache.data
    }

    "encrypt the json payload in the database" in {
      val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(cryptoKey)

      repository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val raw = mongoComponent.database.getCollection[BsonDocument]("read-subscription-records").find().headOption().map(_.value).futureValue
      crypto.decrypt(Crypted(raw.get("data").asString.getValue)).value mustBe userAnswersCache.data
    }
  }

  "get" should {
    "successfully get the record" in {
      repository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      val result = repository.get(userAnswersCache.id).futureValue.value
      result.toString mustBe userAnswersCache.data
    }
  }

  "remove" should {
    "successfully remove the record" in {
      repository.upsert(userAnswersCache.id, Json.parse(userAnswersCache.data)).futureValue
      repository.remove(userAnswersCache.id).futureValue
      repository.get(userAnswersCache.id).futureValue mustBe None

    }
  }

}
