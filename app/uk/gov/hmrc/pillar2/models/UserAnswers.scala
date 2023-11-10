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

package uk.gov.hmrc.pillar2.models

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.pillar2.models.queries.{Gettable, Settable}

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
  id:          String,
  data:        JsObject = Json.obj(),
  lastUpdated: Instant = Instant.now
) {
  import play.api.libs.json.Writes._
  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  // Approach 3: Simplify the set method
  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {
    val updatedData = data.setObject(page.path, Json.toJson(value)(writes)) match {
      case JsSuccess(jsValue, _) => Success(jsValue)
      case JsError(errors)       => Failure(JsResultException(errors))
    }

    updatedData.map(d => copy(data = d))
  }

  // Approach 2: Debug method for setting simple String values
  def debugSetString(page: Settable[String], value: String): Try[UserAnswers] =
    set(page, value)

//  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {
//
//    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
//      case JsSuccess(jsValue, _) =>
//        Success(jsValue)
//      case JsError(errors) =>
//        Failure(JsResultException(errors))
//    }
//
//    updatedData.flatMap { d =>
//      val updatedAnswers = copy(data = d)
//      page.cleanup(Some(value), updatedAnswers)
//    }
//  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(None, updatedAnswers)
    }
  }
}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(unlift(UserAnswers.unapply))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
