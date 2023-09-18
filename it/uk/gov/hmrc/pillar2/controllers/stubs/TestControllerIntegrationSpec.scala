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

package uk.gov.hmrc.pillar2.controllers.stubs

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class TestControllerIntegrationSpec extends AnyWordSpec with Matchers with ScalaFutures with IntegrationPatience with GuiceOneServerPerSuite {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl  = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .build()

  "TestController" should {

    "getAllRecords" should {
      "return all records" in {
        val response = wsClient.url(s"$baseUrl/test/get-all").get().futureValue

        response.status shouldBe OK
      }
    }

    "getRegistrationData" should {
      "return registration data for a valid ID" in {
        val validId = "someValidId"

        // Setup: Create a record with the valid ID
        val setupResponse = wsClient
          .url(s"$baseUrl/test/upsertRecord/$validId")
          .post(Json.obj("key" -> "value"))
          .futureValue
        setupResponse.status shouldBe OK

        // Test: Now try to retrieve the record
        val response = wsClient.url(s"$baseUrl/test/registration-data/$validId").get().futureValue

        response.status shouldBe OK
      }

      "return NotFound for an invalid ID" in {
        val invalidId = "someInvalidId"
        val response  = wsClient.url(s"$baseUrl/test/registration-data/$invalidId").get().futureValue

        response.status shouldBe NOT_FOUND
      }
    }

    "clearCurrentData" should {
      "clear data for a valid ID" in {
        // First, you might need to create a record to delete, depending on your setup
        val validId  = "someValidIdToDelete"
        val response = wsClient.url(s"$baseUrl/test/clear-current/$validId").get().futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.obj("message" -> "Data cleared successfully")
      }
    }

    "clearAllData" should {
      "clear all data successfully" in {
        val response = wsClient.url(s"$baseUrl/test/clear-all").get().futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.obj("message" -> "Data cleared successfully")
      }
    }

    "upsertRecord" in {
      val jsonRequest = Json.obj("key" -> "value")
      val response    = wsClient.url(s"$baseUrl/test/upsertRecord/someId").post(jsonRequest).futureValue

      response.status shouldBe OK
      response.body shouldEqual "Record upserted successfully"
    }

  }
}
