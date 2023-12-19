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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository

import scala.concurrent.{ExecutionContext, Future}

class TestControllerSpec extends BaseSpec {

  // You need to mock the RegistrationCacheRepository since it's a dependency of the controller
  val mockRepository: RegistrationCacheRepository = mock[RegistrationCacheRepository]
  val controller = new TestController(mockRepository, stubControllerComponents())

  "TestController" should {

    "get all records successfully" in {
      when(mockRepository.getAll(any[Int])(any[ExecutionContext])) thenReturn Future.successful(Seq(Json.obj("foo" -> "bar")))

      val result = controller.getAllRecords(10)(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.arr(Json.obj("foo" -> "bar"))
    }

    "handle error while getting all records" in {
      when(mockRepository.getAll(any[Int])(any[ExecutionContext])) thenReturn Future.failed(new RuntimeException("Database error"))

      val result = controller.getAllRecords(10)(fakeRequest)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.obj("message" -> "Database error")
    }

    "get registration data successfully" in {
      when(mockRepository.get(any[String])(any[ExecutionContext])) thenReturn Future.successful(Some(Json.obj("foo" -> "bar")))

      val result = controller.getRegistrationData("someId")(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.obj("foo" -> "bar")
    }

    "handle not found while getting registration data" in {
      when(mockRepository.get(any[String])(any[ExecutionContext])) thenReturn Future.successful(None)

      val result = controller.getRegistrationData("someId")(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
    
    "clear all data successfully" in {
      when(mockRepository.clearAllData()(any[ExecutionContext])) thenReturn Future.successful(true)

      val result = controller.clearAllData()(fakeRequest)

      status(result) shouldBe OK
    }

    "handle error while clearing all data" in {
      when(mockRepository.clearAllData()(any[ExecutionContext])) thenReturn Future.failed(new RuntimeException("Database error"))

      val result = controller.clearAllData()(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "clear current data successfully" in {
      when(mockRepository.remove(any[String])(any[ExecutionContext])) thenReturn Future.successful(true)

      val result = controller.clearCurrentData("someId")(fakeRequest)

      status(result) shouldBe OK
    }

    "handle error while clearing current data" in {
      when(mockRepository.remove(any[String])(any[ExecutionContext])) thenReturn Future.failed(new RuntimeException("Database error"))

      val result = controller.clearCurrentData("someId")(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "upsert record successfully" in {
      // Setup the mock repository to return a successful future when upsert is called
      when(mockRepository.upsert(any[String], any[JsValue])(any[ExecutionContext])) thenReturn Future.successful(())

      // Create a fake request with JSON body
      val request = fakeRequest.withMethod(POST).withJsonBody(Json.obj("foo" -> "bar"))

      // Call the method and check the result
      val result = controller.upsertRecord("someId")(request)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Record upserted successfully"
    }

    "return bad request on invalid JSON" in {
      // Create a fake request with no body (invalid JSON)
      val request = fakeRequest.withMethod(POST)

      // Call the method and check the result
      val result = controller.upsertRecord("someId")(request)

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Invalid JSON"
    }

    "return OK when record is upserted successfully" in {
      // Setup the mock repository to return a successful future when upsert is called
      when(mockRepository.upsert(any[String], any[JsValue])(any[ExecutionContext])) thenReturn Future.successful(())

      // Create a fake request with JSON body
      val request = fakeRequest.withMethod(POST).withJsonBody(Json.obj("foo" -> "bar"))

      // Call the method and check the result
      val result = controller.upsertRecord("someId")(request)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Record upserted successfully"
    }

    "return BadRequest when JSON is invalid" in {
      // Create a fake request with invalid JSON (no body)
      val request = fakeRequest.withMethod(POST)

      // Call the method and check the result
      val result = controller.upsertRecord("someId")(request)

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Invalid JSON"
    }
  }
}
