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
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.repositories.RegistrationDataKeys.lastUpdatedKey

import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

case class RegistrationDataEntry(id: String, data: String, lastUpdated: DateTime)

object RegistrationDataEntryFormats {
  implicit val dateFormat: Format[DateTime]              = MongoJodaFormats.dateTimeFormat
  implicit val format:     Format[RegistrationDataEntry] = Json.format[RegistrationDataEntry]
}
case class JsonDataEntry(id: String, data: JsValue, lastUpdated: DateTime)
object JsonDataEntry {
  implicit val dateFormat: Format[DateTime]      = MongoJodaFormats.dateTimeFormat
  implicit val format:     Format[JsonDataEntry] = Json.format[JsonDataEntry]
}

object RegistrationDataKeys {
  val dataKey:        String = "data"
  val idField:        String = "id"
  val lastUpdatedKey: String = "lastUpdated"
  val expireAtKey:    String = "expireAt"
}

@Singleton
class RegistrationCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  config:         AppConfig
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[RegistrationDataEntry](
      collectionName = "user-answers-records",
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
            .expireAfter(config.defaultDataExpireInSeconds, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("id"),
          IndexOptions().name("id").unique(true).background(false)
        )
      ),
      replaceIndexes = true
    )
    with Logging {

  private def updatedAt: DateTime = DateTime.now(DateTimeZone.UTC)

  private lazy val crypto:  Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(config.registrationCacheCryptoKey)
  private val cryptoToggle: Boolean                  = config.cryptoToggle

  import RegistrationDataEntryFormats._
  import RegistrationDataKeys._

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {

    val encryptedRecord    = RegistrationDataEntry(id, data.toString(), updatedAt)
    val nonEncryptedRecord = JsonDataEntry(id, data, updatedAt)
    val encrypter: Writes[String] = JsonEncryption.stringEncrypter(crypto)
    if (cryptoToggle) {
      val encryptedSetOperation = Updates.combine(
        Updates.set(idField, encryptedRecord.id),
        Updates.set(dataKey, Codecs.toBson(data.toString())(encrypter)),
        Updates.set(lastUpdatedKey, Codecs.toBson(encryptedRecord.lastUpdated))
      )
      collection
        .withDocumentClass[RegistrationDataEntry]()
        .findOneAndUpdate(filter = Filters.eq(idField, id), update = encryptedSetOperation, new FindOneAndUpdateOptions().upsert(true))
        .toFuture()
        .map(_ => ())
    } else {
      val setOperation = Updates.combine(
        Updates.set(idField, nonEncryptedRecord.id),
        Updates.set(dataKey, Codecs.toBson(nonEncryptedRecord.data)),
        Updates.set(lastUpdatedKey, Codecs.toBson(nonEncryptedRecord.lastUpdated))
      )
      collection
        .withDocumentClass[JsonDataEntry]()
        .findOneAndUpdate(filter = Filters.eq(idField, id), update = setOperation, new FindOneAndUpdateOptions().upsert(true))
        .toFuture()
        .map(_ => ())
    }

  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] =
    if (cryptoToggle) {
      collection.find[RegistrationDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map { dataEntry =>
          Json.parse(crypto.decrypt(Crypted(dataEntry.data)).value)
        }
      }
    } else {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map { dataEntry =>
          dataEntry.data
        }
      }
    }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] =
    collection.find(Filters.equal(idField, id)).headOption().map {
      _.map { dataEntry =>
        dataEntry.lastUpdated
      }
    }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.deleteOne(Filters.equal(idField, id)).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName externalId:$id")
      result.wasAcknowledged
    }

  def getAll(max: Int)(implicit ec: ExecutionContext): Future[Seq[JsValue]] =
    (if (cryptoToggle) {
       collection
         .find()
         .map { dataEntry =>
           Json.parse(crypto.decrypt(Crypted(dataEntry.data)).value)
         }
     } else {
       collection
         .find[JsonDataEntry]()
         .map { dataEntry =>
           dataEntry.data
         }
     }).toFuture()

  def clearAllData()(implicit ec: ExecutionContext): Future[Boolean] =
    collection.deleteMany(BsonDocument()).toFuture().map { result =>
      logger.info(s"Removing all the rows from collection $collectionName")
      result.wasAcknowledged
    }
}
