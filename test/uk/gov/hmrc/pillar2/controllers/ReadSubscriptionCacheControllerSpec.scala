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

import org.apache.commons.lang3.RandomUtils
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository

import scala.concurrent.{ExecutionContext, Future}

class ReadSubscriptionCacheControllerSpec extends BaseSpec {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]
  trait Setup {
    val controller =
      new ReadSubscriptionCacheController(
        mockedCache,
        mockAuthAction,
        stubControllerComponents()
      )
  }

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[ReadSubscriptionCacheRepository].toInstance(mockedCache),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "save" - {
    "return 200 when request is saved successfully" in new Setup {
      when(mockedCache.upsert(any[String](), any[JsValue]())(any[ExecutionContext]())).thenReturn(Future.successful((): Unit))
      val request = FakeRequest(POST, routes.ReadSubscriptionCacheController.save("id").url).withJsonBody(Json.obj("abc" -> "def"))
      val result  = route(application, request).value
      status(result) mustBe OK
    }

    "throw exception when mongo is down" in new Setup {

      val request = FakeRequest(POST, routes.ReadSubscriptionCacheController.save("id").url).withRawBody(ByteString(RandomUtils.nextBytes(512001)))
      val result  = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE

    }
  }
  "get" - {
    "return 200 when data exists" in new Setup {
      val jsonObject = Json.obj("hello" -> "goodbye")
      when(mockedCache.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(Some(jsonObject)))
      val request = FakeRequest(GET, routes.ReadSubscriptionCacheController.get("id").url)
      val result  = route(application, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe jsonObject

    }
    "return NOT_FOUND when data exists" in new Setup {
      when(mockedCache.get(any[String]())(any[ExecutionContext]())).thenReturn(Future.successful(None))
      val request = FakeRequest(GET, routes.ReadSubscriptionCacheController.get("id").url)
      val result  = route(application, request).value

      status(result) mustBe NOT_FOUND

    }
    "remove" - {
      "return 200 when the record is removed successfully" in new Setup {
        when(mockedCache.remove(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful(true)
        val request = FakeRequest(DELETE, routes.ReadSubscriptionCacheController.remove("id").url)
        val result  = route(application, request).value
        status(result) mustBe OK
      }

      "return InternalServerError if the record is not removed" in new Setup {
        when(mockedCache.remove(eqTo("id"))(any[ExecutionContext]())) thenReturn Future.successful(false)
        val request = FakeRequest(DELETE, routes.ReadSubscriptionCacheController.remove("id").url)
        val result  = route(application, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
