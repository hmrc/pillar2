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

import akka.util.ByteString
import org.apache.commons.lang3.RandomUtils
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsSuccess, Json, Reads}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, SimpleRetrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, CredentialRole, Enrolment, EnrolmentIdentifier, Enrolments, User}
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.controllers.auth.FakeAuthAction
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository

import scala.concurrent.Future

class RegistrationCacheControllerSpec extends BaseSpec {
  trait Setup {
    val controller =
      new RegistrationCacheController(
        mockRgistrationCacheRepository,
        mockAuthAction,
        stubControllerComponents()
      )
  }

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false))
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRgistrationCacheRepository),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  private val internalId = "externalId"
  private val enrolments = Enrolments(
    Set(
      Enrolment(
        "HMRC-PILLAR2-ORG",
        Seq(
          EnrolmentIdentifier("PLRID", "A0000000")
        ),
        "Activated",
        None
      )
    )
  )

  "save" - {
    "return 200 when request is saved successfully" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)
//      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ AffinityGroup ~ CredentialRole](any(), any())(any(), any())) thenReturn Future.successful(
//        new ~(Some(internalId), enrolments , Some(AffinityGroup.Organisation) , Some(User))
//      )
//      when(mockAuthAction.authorised(eqTo(EmptyPredicate) ,eqTo(AuthProviderId.retrieval))(any(), any()))
//        .thenReturn(Future.successful("1234"))
      val request = FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withJsonBody(Json.obj("abc" -> "def"))
      val result  = route(application, request).value
      status(result) mustBe OK
    }

    "return 413 when request is not right" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)
      val request = FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withRawBody(ByteString(RandomUtils.nextBytes(512001)))
      val result  = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE
    }
    "throw exception when mongo is down" in new Setup {
      when(mockRgistrationCacheRepository.upsert(any(), any())(any())) thenReturn Future.failed(new Exception(""))

      val request = FakeRequest(POST, routes.RegistrationCacheController.save("id").url).withRawBody(ByteString(RandomUtils.nextBytes(512001)))
      val result  = route(application, request).value

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE

    }
  }
  "get" - {
    "return 200 when data exists" in new Setup {
      when(mockRgistrationCacheRepository.get(eqTo("id"))(any())) thenReturn Future.successful {
        Some(Json.obj())
      }
      val request = FakeRequest(GET, routes.RegistrationCacheController.get("id").url)
      val result  = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "{}"

    }
    "return NOT_FOUND when data exists" in new Setup {
      when(mockRgistrationCacheRepository.get(eqTo("id"))(any())) thenReturn Future.successful {
        None
      }

      val request = FakeRequest(GET, routes.RegistrationCacheController.get("id").url)
      val result  = route(application, request).value

      status(result) mustBe NOT_FOUND

    }
    "remove" - {
      "return 200 when the record is removed successfully" in new Setup {
        when(mockRgistrationCacheRepository.remove(eqTo("id"))(any())) thenReturn Future.successful(true)

        val request = FakeRequest(DELETE, routes.RegistrationCacheController.remove("id").url)
        val result  = route(application, request).value

        status(result) mustBe OK
      }

    }
    "lastUpdated" - {
      "return 200 and if record when it exists" in new Setup {
        val date = DateTime.now
        when(mockRgistrationCacheRepository.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(date)
        }

        val request = FakeRequest(GET, routes.RegistrationCacheController.lastUpdated("foo").url)
        val result  = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(date.getMillis)
      }

      "return 404 when the data doesn't exist" in new Setup {
        when(mockRgistrationCacheRepository.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }

        val request = FakeRequest(POST, routes.RegistrationCacheController.lastUpdated("id").url)
        val result  = route(application, request).value

        status(result) mustBe NOT_FOUND
      }
    }
  }
}
