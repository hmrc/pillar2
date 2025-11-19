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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AnyContentAsJson
import play.api.mvc.AnyContentAsRaw
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random.nextBytes

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
      when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.successful((): Unit))
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, routes.ReadSubscriptionCacheController.save("id").url).withJsonBody(Json.obj("abc" -> "def"))
      val result: Future[Result] = route(application, request).value
      status(result) mustBe OK
    }

    "throw exception when mongo is down" in new Setup {

      val request: FakeRequest[AnyContentAsRaw] =
        FakeRequest(POST, routes.ReadSubscriptionCacheController.save("id").url).withRawBody(ByteString(nextBytes(512001)))
      val result: Future[Result] = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE

    }
  }
  "get" - {
    "return 200 when data exists" in new Setup {
      val jsonObject: JsObject = Json.obj("hello" -> "goodbye")
      when(mockedCache.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsonObject)))
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.ReadSubscriptionCacheController.get("id").url)
      val result:  Future[Result]                      = route(application, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe jsonObject

    }
    "return NOT_FOUND when data exists" in new Setup {
      when(mockedCache.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(None))
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.ReadSubscriptionCacheController.get("id").url)
      val result:  Future[Result]                      = route(application, request).value

      status(result) mustBe NOT_FOUND

    }
    "remove" - {
      "return 200 when the record is removed successfully" in new Setup {
        when(mockedCache.remove(eqTo("id"))(using any[ExecutionContext]())) thenReturn Future.successful(true)
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.ReadSubscriptionCacheController.remove("id").url)
        val result:  Future[Result]                      = route(application, request).value
        status(result) mustBe OK
      }

      "return InternalServerError if the record is not removed" in new Setup {
        when(mockedCache.remove(eqTo("id"))(using any[ExecutionContext]())) thenReturn Future.successful(false)
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.ReadSubscriptionCacheController.remove("id").url)
        val result:  Future[Result]                      = route(application, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
