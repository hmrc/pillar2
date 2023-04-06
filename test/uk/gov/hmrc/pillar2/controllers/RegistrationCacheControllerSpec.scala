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

import akka.stream.Materializer
import akka.util.ByteString
import org.apache.commons.lang3.RandomUtils
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers.{call, contentAsJson, contentAsString, status, stubControllerComponents}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import play.api.test.FakeRequest

import scala.concurrent.Future

class RegistrationCacheControllerSpec extends BaseSpec {
  trait Setup {
    val controller =
      new RegistrationCacheController(
        mockRgistrationCacheRepository,
        stubControllerComponents()
      )
  }

  "save" when {
    "return 200 when request is saved successfully" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)

      val result = controller.save("id")(FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))
      status(result) shouldBe OK
    }

    "return 413 when request is not right" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)

      val result = controller.save("foo")(FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

      status(result) shouldBe REQUEST_ENTITY_TOO_LARGE
    }
    "throw exception when mongo is down" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.failed(new Exception(""))

      val result = controller.save("foo")(FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

    }
  }
  "get" when {
    "return 200 when data exists" in new Setup {
      when(mockRgistrationCacheRepository.get(eqTo("id"))(any())) thenReturn Future.successful {
        Some(Json.obj())
      }
      val result = controller.get("id")(FakeRequest())

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "{}"

    }
    "return NOT_FOUND when data exists" in new Setup {
      when(mockRgistrationCacheRepository.get(eqTo("id"))(any())) thenReturn Future.successful {
        None
      }
      val result = controller.get("id")(FakeRequest())

      status(result) shouldBe NOT_FOUND

    }
    "remove" when {
      "return 200 when the record is removed successfully" in new Setup {
        when(mockRgistrationCacheRepository.remove(eqTo("id"))(any())) thenReturn Future.successful(true)

        val result = controller.remove("id")(FakeRequest())

        status(result) shouldBe OK
      }

    }
    "lastUpdated" when {
      "return 200 and if record when it exists" in new Setup {
        val date = DateTime.now
        when(mockRgistrationCacheRepository.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(date)
        }

        val result = controller.lastUpdated("foo")(FakeRequest())

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(date.getMillis)
      }

      "return 404 when the data doesn't exist" in new Setup {
        when(mockRgistrationCacheRepository.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }

        val result = controller.lastUpdated("foo")(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }
  }
}
