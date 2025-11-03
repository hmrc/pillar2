/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.service

import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.errors.{ApiInternalServerError, ETMPValidationError, InvalidJsonError}

import java.time.ZonedDateTime

class GenericServiceSpec extends BaseSpec with ScalaCheckDrivenPropertyChecks with ScalaFutures {

  case class DummyType(someInt: Int)

  object DummyType {
    implicit val reads: Reads[DummyType] = Json.reads
  }

  "generic response mapping" - {

    "when handling a 200 or 201" - {

      val dummyValue        = 10
      val dummyInstance     = DummyType(dummyValue)
      val dummyInstanceJson = Json.obj("someInt" -> dummyValue)

      val okOrCreated = Gen.oneOf(200, 201)

      "with a valid JSON body" - {
        "returns a successful future" in forAll(okOrCreated) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, dummyInstanceJson.toString()))
          result.futureValue mustBe dummyInstance
        }
      }

      "with an unparseable JSON body" - {
        "fails the future with an invalid json error" in forAll(okOrCreated) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, Json.obj().toString()))
          result.failed.futureValue mustBe an[InvalidJsonError]
        }
      }

      "with a non-JSON body" - {
        "fails the future" in forAll(okOrCreated) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, "not json"))
          result.failed.futureValue mustBe an[InvalidJsonError]
        }
      }

    }

    "when handling a 422" - {

      val unprocessableErrorCode      = "some test error code"
      val unprocessableMessage        = "some fake error message"
      val unprocessableProcessingDate = "2022-01-31T09:26:17Z"
      val unprocessableJson = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> unprocessableProcessingDate,
          "code"           -> unprocessableErrorCode,
          "text"           -> unprocessableMessage
        )
      )
      val unprocessableModelledError =
        ETMPValidationError(unprocessableErrorCode, unprocessableMessage, ZonedDateTime.parse(unprocessableProcessingDate))

      "with a valid JSON body" - {
        "returns a failed future with a modelled error" in {
          val result = convertToResult[DummyType](HttpResponse(422, unprocessableJson.toString()))
          result.failed.futureValue mustBe unprocessableModelledError
        }
      }

      "with an unparseable JSON body" - {
        "fails the future with an invalid json error" in {
          val result = convertToResult[DummyType](HttpResponse(422, Json.obj().toString()))
          result.failed.futureValue mustBe InvalidJsonError(Json.obj().toString())
        }
      }

      "with a non-JSON body" - {
        "fails the future" in {
          val result = convertToResult[DummyType](HttpResponse(422, "not json"))
          result.failed.futureValue mustBe an[InvalidJsonError]
        }
      }
    }

    "when handling a 400 or 500" - {

      val errorCode    = "some test error code"
      val errorMessage = "some fake error message"
      val errorJson = Json.obj(
        "error" -> Json.obj(
          "code"    -> errorCode,
          "message" -> errorMessage,
          "logID"   -> "unused"
        )
      )
      val modelledError = ApiInternalServerError(errorMessage, errorCode)

      val expectedErrorStatuses = Gen.oneOf(400, 500)

      "with a valid JSON body" - {
        "returns a failed future with a modelled error" in forAll(expectedErrorStatuses) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, errorJson.toString()))
          result.failed.futureValue mustBe modelledError
        }
      }

      "with an unparseable JSON body" - {
        "fails the future with a default internal server error" in forAll(expectedErrorStatuses) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, Json.obj().toString()))
          result.failed.futureValue mustBe ApiInternalServerError.defaultInstance
        }
      }

      "with a non-JSON body" - {
        "fails the future" in forAll(expectedErrorStatuses) { httpStatus =>
          val result = convertToResult[DummyType](HttpResponse(httpStatus, "not json"))
          result.failed.futureValue mustBe ApiInternalServerError.defaultInstance
        }
      }

    }

  }

}
