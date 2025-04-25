/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.{BaseSpec, ORNDataFixture}

class ORNConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ORNDataFixture {

  override lazy val app: Application = applicationBuilder()
    .configure("microservice.services.overseas-return-notification.port" -> server.port())
    .build()

  private lazy val connector = app.injector.instanceOf[ORNConnector]
  private val POST           = "POST"
  private val PUT            = "PUT"

  val getUrl: String =
    s"/RESTAdapter/plr/overseas-return-notification?accountingPeriodFrom=${fromDate.toString}&accountingPeriodTo=${toDate.toString}"

  private def stubResponseFor(status: Int, method: String)(implicit response: JsObject): StubMapping = {
    val requestBuilder = method match {
      case "POST" => post(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
      case "PUT"  => put(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
    }

    server.stubFor(
      requestBuilder
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .withRequestBody(equalToJson(ornRequestJson.toString()))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(response.toString())
        )
    )
  }

  private def stubResponseForGet(status: Int): StubMapping =
    server.stubFor(
      get(urlEqualTo(getUrl))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(Json.stringify(Json.toJson(ornResponse)))
        )
    )

  "submitOrn" - {
    "successfully submit a ORN request with X-PILLAR2-Id and receive Success response" in {
      implicit val response: JsObject = Json.obj(
        "success" -> Json.obj(
          "processingDate"   -> "2024-03-14T09:26:17Z",
          "formBundleNumber" -> "123456789012345"
        )
      )

      stubResponseFor(CREATED, POST)

      val result = connector.submitOrn(ornRequest).futureValue

      result.status mustBe CREATED
      result.json mustBe response
      server.verify(
        postRequestedFor(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withRequestBody(equalToJson(Json.toJson(ornRequest).toString()))
      )
    }

    "handle BAD_REQUEST (400) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "400",
          "message" -> "Bad Request",
          "logId"   -> "123456789"
        )
      )

      stubResponseFor(BAD_REQUEST, POST)

      val result = connector.submitOrn(ornRequest).futureValue
      result.status mustBe BAD_REQUEST
      result.json mustBe response
    }

    "handle UNPROCESSABLE_ENTITY (422) response" in {
      implicit val response: JsObject = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z",
          "code"           -> "003",
          "text"           -> "Request could not be processed"
        )
      )

      stubResponseFor(UNPROCESSABLE_ENTITY, POST)

      val result = connector.submitOrn(ornRequest).futureValue
      result.status mustBe UNPROCESSABLE_ENTITY
      result.json mustBe response
    }

    "handle INTERNAL_SERVER_ERROR (500) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal Server Error",
          "logId"   -> "123456789"
        )
      )

      stubResponseFor(INTERNAL_SERVER_ERROR, POST)

      val result = connector.submitOrn(ornRequest).futureValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe response
    }
  }

  "amendOrn" - {
    "successfully amend an ORN request with X-PILLAR2-ID and receive a Success response" in {
      implicit val response: JsObject = Json.obj(
        "success" -> Json.obj(
          "processingDate"   -> "2024-03-15T016:07:22Z",
          "formBundleNumber" -> "123456789012345"
        )
      )

      stubResponseFor(OK, PUT)

      val result = connector.amendOrn(ornRequest).futureValue

      result.status mustBe OK
      result.json mustBe response
      server.verify(
        putRequestedFor(urlEqualTo("/RESTAdapter/plr/overseas-return-notification"))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withRequestBody(equalToJson(Json.toJson(ornRequest).toString()))
      )
    }
    "handle BAD_REQUEST (400) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "400",
          "message" -> "Bad Request",
          "logId"   -> "123456789"
        )
      )

      stubResponseFor(BAD_REQUEST, PUT)

      val result = connector.amendOrn(ornRequest).futureValue
      result.status mustBe BAD_REQUEST
      result.json mustBe response
    }

    "handle UNPROCESSABLE_ENTITY (422) response" in {
      implicit val response: JsObject = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z",
          "code"           -> "003",
          "text"           -> "Request could not be processed"
        )
      )

      stubResponseFor(UNPROCESSABLE_ENTITY, PUT)

      val result = connector.amendOrn(ornRequest).futureValue
      result.status mustBe UNPROCESSABLE_ENTITY
      result.json mustBe response
    }

    "handle INTERNAL_SERVER_ERROR (500) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal Server Error",
          "logId"   -> "123456789"
        )
      )

      stubResponseFor(INTERNAL_SERVER_ERROR, PUT)

      val result = connector.amendOrn(ornRequest).futureValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe response
    }
  }

  "getOrn" - {
    "successfully get a ORN with X-PILLAR2-Id and receive Success response" in {
      stubResponseForGet(OK)

      val result = connector.getOrn(fromDate, toDate).futureValue

      result.status mustBe OK
      verifyHipHeaders("GET", getUrl)
    }

    "must return status as 500 when ORN is not returned" in {

      stubResponseForGet(INTERNAL_SERVER_ERROR)

      val result = connector.getOrn(fromDate, toDate).failed

      result.failed.map { ex =>
        ex mustBe a[InternalServerException]
      }
    }
  }
}
