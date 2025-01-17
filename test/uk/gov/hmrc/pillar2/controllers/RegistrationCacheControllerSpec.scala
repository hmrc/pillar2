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

import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random.nextBytes

class RegistrationCacheControllerSpec extends BaseSpec {
  trait Setup {
    val controller =
      new RegistrationCacheController(
        mockRegistrationCacheRepository,
        mockAuthAction,
        stubControllerComponents()
      )
  }

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRegistrationCacheRepository),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "save" - {
    "return 200 when request is saved successfully" in new Setup {
      when(mockRegistrationCacheRepository.upsert(any[String](), any[JsValue]())(any[ExecutionContext]())) thenReturn Future.successful((): Unit)
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withJsonBody(Json.obj("abc" -> "def"))
      val result: Future[Result] = route(application, request).value
      status(result) mustBe OK
    }

    "return 413 when request is not right" in new Setup {
      when(mockRegistrationCacheRepository.upsert(any[String](), any[JsValue]())(any[ExecutionContext]())) thenReturn Future.successful((): Unit)
      val request: FakeRequest[AnyContentAsRaw] =
        FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withRawBody(ByteString(nextBytes(512001)))
      val result: Future[Result] = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE
    }
    "throw exception when mongo is down" in new Setup {
      when(mockRegistrationCacheRepository.upsert(any[String](), any[JsValue]())(any[ExecutionContext]())) thenReturn Future.failed(new Exception(""))

      val request: FakeRequest[AnyContentAsRaw] =
        FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withRawBody(ByteString(nextBytes(512001)))
      val result: Future[Result] = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE

    }
  }
  "get" - {
    "return 200 when data exists" in new Setup {
      when(mockRegistrationCacheRepository.get(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful {
        Some(Json.obj())
      }
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.RegistrationCacheController.get("id").url)
      val result:  Future[Result]                      = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "{}"

    }
    "return NOT_FOUND when data exists" in new Setup {
      when(mockRegistrationCacheRepository.get(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful {
        None
      }

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.RegistrationCacheController.get("id").url)
      val result:  Future[Result]                      = route(application, request).value

      status(result) mustBe NOT_FOUND

    }
    "remove" - {
      "return 200 when the record is removed successfully" in new Setup {
        when(mockRegistrationCacheRepository.remove(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful(true)

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.RegistrationCacheController.remove("id").url)
        val result:  Future[Result]                      = route(application, request).value

        status(result) mustBe OK
      }
      "return InternalServerError if the record is not removed successfully" in new Setup {
        when(mockRegistrationCacheRepository.remove(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful(false)

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.RegistrationCacheController.remove("id").url)
        val result:  Future[Result]                      = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
    "lastUpdated" - {
      "return 200 and if record when it exists" in new Setup {
        val date: Instant = Instant.now
        when(mockRegistrationCacheRepository.getLastUpdated(eqTo("foo"))(any[ExecutionContext]())) thenReturn Future.successful {
          Some(date)
        }

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.RegistrationCacheController.lastUpdated("foo").url)
        val result:  Future[Result]                      = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(date.getEpochSecond)
      }

      "return 404 when the data doesn't exist" in new Setup {
        when(mockRegistrationCacheRepository.getLastUpdated(eqTo("foo"))(any[ExecutionContext]())) thenReturn Future.successful {
          None
        }

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, routes.RegistrationCacheController.lastUpdated("id").url)
        val result:  Future[Result]                      = route(application, request).value

        status(result) mustBe NOT_FOUND
      }
    }
  }
}
