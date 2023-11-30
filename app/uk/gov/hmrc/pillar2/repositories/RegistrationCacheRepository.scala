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
import org.mongodb.scala.bson.{BsonBinary, BsonDocument}
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoBinaryFormats.{byteArrayReads, byteArrayWrites}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.repositories.RegistrationDataEntry.RegistrationDataEntryFormats.expireAtKey
import uk.gov.hmrc.pillar2.repositories.RegistrationDataEntry.{DataEntry, JsonDataEntry, RegistrationDataEntry, RegistrationDataEntryFormats}

import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

object RegistrationDataEntry {

  sealed trait RegistrationDataEntry

  case class DataEntry(id: String, data: BsonBinary, lastUpdated: DateTime, expireAt: DateTime) extends RegistrationDataEntry

  case class JsonDataEntry(id: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime) extends RegistrationDataEntry

  object DataEntry {
    def apply(id: String, data: Array[Byte], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC), expireAt: DateTime): DataEntry =
      DataEntry(id, BsonBinary(data), lastUpdated, expireAt)

    final val bsonBinaryReads:     Reads[BsonBinary]  = byteArrayReads.map(t => BsonBinary(t))
    final val bsonBinaryWrites:    Writes[BsonBinary] = byteArrayWrites.contramap(t => t.getData)
    implicit val bsonBinaryFormat: Format[BsonBinary] = Format(bsonBinaryReads, bsonBinaryWrites)

    implicit val dateFormat: Format[DateTime]  = MongoJodaFormats.dateTimeFormat
    implicit val format:     Format[DataEntry] = Json.format[DataEntry]
  }

  object JsonDataEntry {
    implicit val dateFormat: Format[DateTime]      = MongoJodaFormats.dateTimeFormat
    implicit val format:     Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  object RegistrationDataEntryFormats {
    implicit val dateFormat: Format[DateTime]              = MongoJodaFormats.dateTimeFormat
    implicit val format:     Format[RegistrationDataEntry] = Json.format[RegistrationDataEntry]

    val dataKey:        String = "data"
    val idField:        String = "id"
    val lastUpdatedKey: String = "lastUpdated"
    val expireAtKey:    String = "expireAt"
  }
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
        Codecs.playFormatCodec(JsonDataEntry.format),
        Codecs.playFormatCodec(DataEntry.format)
      ),
      indexes = Seq(
        IndexModel(
          Indexes.ascending(expireAtKey),
          IndexOptions()
            .name("dataExpiry")
            .expireAfter(2419200, TimeUnit.SECONDS)
            .background(true)
        )
      ),
      replaceIndexes = true
    )
    with Logging {

  import RegistrationDataEntryFormats._

  private def getExpireAt: DateTime =
    DateTime
      .now(DateTimeZone.UTC)
      .toLocalDate
      .plusDays(config.defaultDataExpireInDays + 1)
      .toDateTimeAtStartOfDay()

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {

    val record = JsonDataEntry(id, data, DateTime.now(DateTimeZone.UTC), getExpireAt)
    val setOperation = Updates.combine(
      Updates.set(idField, record.id),
      Updates.set(dataKey, Codecs.toBson(record.data)),
      Updates.set(lastUpdatedKey, Codecs.toBson(record.lastUpdated)),
      Updates.set(expireAtKey, Codecs.toBson(record.expireAt))
    )
    collection
      .withDocumentClass[JsonDataEntry]()
      .findOneAndUpdate(filter = Filters.eq(idField, id), update = setOperation, new FindOneAndUpdateOptions().upsert(true))
      .toFuture()
      .map(_ => ())

  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] =
    collection
      .find[JsonDataEntry](Filters.equal(idField, id))
      .headOption()
      .map {
        case Some(dataEntry) =>
          println(s"Fetched data for id: $id, data: ${dataEntry.data}")
          Some(dataEntry.data)
        case None =>
          println(s"No data found for id: $id")
          None
      }
      .recover { case ex: Throwable =>
        println(s"Error fetching data for id: $id", ex)
        None
      }
//  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] =
//    collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
//      _.map { dataEntry =>
//        dataEntry.data
//      }
//    }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] =
    collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
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
    collection
      .find[JsonDataEntry]()
      .map { dataEntry =>
        dataEntry.data
      }
      .toFuture()

  def clearAllData()(implicit ec: ExecutionContext): Future[Boolean] =
    collection.deleteMany(BsonDocument()).toFuture().map { result =>
      logger.info(s"Removing all the rows from collection $collectionName")
      result.wasAcknowledged
    }

}
