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

package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.{JsResultException, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{ETMPAmendSubscriptionSuccess, SubscriptionResponse}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail

class SubscriptionConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with IntegrationPatience {
  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.create-subscription.port" -> server.port()
    )
    .build()
  private val errorCodes: Gen[Int] = Gen.oneOf(Seq(203, 204, 400, 403, 500, 501, 502, 503, 504))
  lazy val connector: SubscriptionConnector =
    app.injector.instanceOf[SubscriptionConnector]

  "SubscriptionConnector" - {

    "for a Create Subscription" - {
      "must return status as OK" in {

        forAll(arbitrary[RequestDetail]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            OK
          )
          val result = await(connector.sendCreateSubscriptionInformation(sub))
          result.status mustBe OK
        }
      }

      "must return status as BAD_REQUEST" in {

        forAll(arbitrary[RequestDetail]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            BAD_REQUEST
          )

          val result = connector.sendCreateSubscriptionInformation(sub).futureValue
          result.status mustBe BAD_REQUEST
        }
      }

      "must return status as INTERNAL_SERVER_ERROR" in {

        forAll(arbitrary[RequestDetail]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            INTERNAL_SERVER_ERROR
          )

          val result = connector.sendCreateSubscriptionInformation(sub).futureValue
          result.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "for retrieving Subscription Information" - {

      "must return object when the response was OK" in {
        forAll(arbPlrReference.arbitrary, arbitrarySubscriptionResponse.arbitrary) { (plrReference, response) =>
          server.stubFor(
            get(urlEqualTo(s"/pillar2/subscription/$plrReference"))
              .willReturn(
                aResponse()
                  .withStatus(200)
                  .withBody(Json.stringify(Json.toJson(SubscriptionResponse(response.success))))
              )
          )
          val result = connector.getSubscriptionInformation(plrReference).futureValue
          result.status mustEqual OK
          result.json mustEqual Json.toJson(SubscriptionResponse(response.success))
        }
      }
      "must throw exception when unexpected body is received" in {
        forAll(arbPlrReference.arbitrary) { plrReference =>
          server.stubFor(
            get(urlEqualTo(s"/pillar2/subscription/$plrReference"))
              .willReturn(
                aResponse()
                  .withStatus(200)
                  .withBody(Json.stringify(Json.obj()))
              )
          )
          val result = connector.getSubscriptionInformation(plrReference)
          result.failed.map { ex =>
            ex shouldBe a[JsResultException]
          }
        }
      }

      "must return future failed for non-200 responses" in {
        val errorResponse = errorCodes.sample.value
        forAll(plrReferenceGen) { (plrReference: String) =>
          stubGetResponse(
            s"/pillar2/subscription/$plrReference",
            errorResponse
          )

          val result = connector.getSubscriptionInformation(plrReference)
          result.futureValue.status mustBe errorResponse
        }
      }

    }

    "amendSubscriptionInformation" - {

      "must return status as OK for a successful amendment" in {
        forAll(arbitrary[ETMPAmendSubscriptionSuccess]) { amendRequest =>
          stubPutResponse(
            s"/pillar2/subscription",
            OK
          )

          val result = await(connector.amendSubscriptionInformation(amendRequest))
          result.status mustBe OK
        }
      }

      "should handle 400 Bad Request" in {
        forAll { (amendRequest: ETMPAmendSubscriptionSuccess) =>
          stubPutResponse("/pillar2/subscription", BAD_REQUEST)

          val result = await(connector.amendSubscriptionInformation(amendRequest))

          result.status mustBe BAD_REQUEST
        }
      }

      "should handle exceptions" in {
        forAll { (amendRequest: ETMPAmendSubscriptionSuccess) =>
          server.stop()

          val exception = intercept[Throwable] {
            await(connector.amendSubscriptionInformation(amendRequest))
          }

          exception mustBe a[Throwable]
          server.start()
        }
      }
    }

  }

}
