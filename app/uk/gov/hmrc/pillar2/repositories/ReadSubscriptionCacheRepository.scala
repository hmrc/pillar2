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

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import org.mongodb.scala.model.*
import play.api.Logging
import play.api.libs.json.*
import uk.gov.hmrc.crypto.*
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.repositories.RegistrationDataKeys.*

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReadSubscriptionCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  config:         AppConfig
)(using
  ec: ExecutionContext
) extends PlayMongoRepository[RegistrationDataEntry](
      collectionName = "read-subscription-records",
      mongoComponent = mongoComponent,
      domainFormat = RegistrationDataEntryFormats.format,
      extraCodecs = Seq(
        Codecs.playFormatCodec(JsonDataEntry.format)
      ),
      indexes = Seq(
        IndexModel(
          Indexes.ascending(lastUpdatedKey),
          IndexOptions()
            .name("lastUpdatedIndex")
            .expireAfter(900, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("id"),
          IndexOptions().name("id").unique(true).background(false)
        )
      ),
      replaceIndexes = true
    )
    with Logging {

  private def updatedAt: Instant = Instant.now

  private lazy val crypto:  Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(config.registrationCacheCryptoKey)
  private val cryptoToggle: Boolean                  = config.cryptoToggle

  def upsert(id: String, data: JsValue)(using ec: ExecutionContext): Future[Unit] =
    if cryptoToggle then
      val encryptedRecord = RegistrationDataEntry(id, data.toString(), updatedAt)
      val encrypter:     Writes[String] = JsonEncryption.stringEncrypter(crypto)
      val encryptedData: String         = encrypter.writes(data.toString()).as[String]
      val encryptedUpdate = Updates.combine(
        Updates.set(idField, encryptedRecord.id),
        Updates.set(dataKey, Codecs.toBson(encryptedData)),
        Updates.set(lastUpdatedKey, Codecs.toBson(encryptedRecord.lastUpdated)(using RegistrationDataEntryFormats.dateFormat))
      )

      collection
        .withDocumentClass[RegistrationDataEntry]()
        .findOneAndUpdate(Filters.eq(idField, id), update = encryptedUpdate, new FindOneAndUpdateOptions().upsert(true))
        .toFuture()
        .map(_ => ())
    else
      val nonEncryptedRecord = JsonDataEntry(id, data, updatedAt)
      val update = Updates.combine(
        Updates.set(idField, nonEncryptedRecord.id),
        Updates.set(dataKey, Codecs.toBson(nonEncryptedRecord.data)),
        Updates.set(lastUpdatedKey, Codecs.toBson(nonEncryptedRecord.lastUpdated)(using JsonDataEntry.dateFormat))
      )

      collection
        .withDocumentClass[JsonDataEntry]()
        .findOneAndUpdate(filter = Filters.eq(idField, id), update = update, new FindOneAndUpdateOptions().upsert(true))
        .toFuture()
        .map(_ => ())

  def get(id: String)(using ec: ExecutionContext): Future[Option[JsValue]] =
    if cryptoToggle then {
      collection.find[RegistrationDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map { dataEntry =>
          Json.parse(crypto.decrypt(Crypted(dataEntry.data)).value)
        }
      }
    } else {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map(_.data)
      }
    }

  def remove(id: String)(using ec: ExecutionContext): Future[Boolean] =
    collection.deleteOne(Filters.equal(idField, id)).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName externalId:$id")
      result.wasAcknowledged
    }

}
