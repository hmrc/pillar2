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
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.btn.BTNRequest

import java.time.LocalDate

class BTNConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure("microservice.services.below-threshold-notification.port" -> server.port())
    .build()

  private lazy val connector = app.injector.instanceOf[BTNConnector]

  private val btnPayload =
    BTNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1)
    )

  private val etmpBTNUrl: String = "/RESTAdapter/plr/below-threshold-notification"

  private def stubResponseFor(status: Int)(implicit response: JsObject): StubMapping =
    server.stubFor(
      post(urlEqualTo("/RESTAdapter/plr/below-threshold-notification"))
        .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
        .withRequestBody(equalToJson(Json.toJson(btnPayload).toString()))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(response.toString())
        )
    )

  "sendBtn" - {
    "successfully submit a BTN request with all required HIP headers" in {
      implicit val response: JsObject = Json.obj(
        "success" -> Json.obj(
          "processingDate" -> "2024-03-14T09:26:17Z"
        )
      )

      stubResponseFor(CREATED)

      val result = connector.sendBtn(btnPayload).futureValue

      result.status mustBe CREATED
      result.json mustBe response
      server.verify(
        postRequestedFor(urlEqualTo("/RESTAdapter/plr/below-threshold-notification"))
          .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
          .withRequestBody(equalToJson(Json.toJson(btnPayload).toString()))
      )
      verifyHipHeaders("POST", etmpBTNUrl, Some(Json.toJson(btnPayload).toString()))
    }

    "handle BAD_REQUEST (400) response" in {
      implicit val response: JsObject = Json.obj(
        "error" -> Json.obj(
          "code"    -> "400",
          "message" -> "Bad Request",
          "logId"   -> "123456789"
        )
      )

      stubResponseFor(BAD_REQUEST)

      val result = connector.sendBtn(btnPayload).futureValue
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

      stubResponseFor(UNPROCESSABLE_ENTITY)

      val result = connector.sendBtn(btnPayload).futureValue
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

      stubResponseFor(INTERNAL_SERVER_ERROR)

      val result = connector.sendBtn(btnPayload).futureValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe response
    }
  }
}
