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

import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{AmendSubscriptionInput, AmendSubscriptionResponse, AmendSubscriptionSuccess}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail

class SubscriptionConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.create-subscription.port" -> server.port()
    )
    .build()

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

      "must return status as OK" in {
        forAll(plrReferenceGen) { (plrReference: String) =>
          stubGetResponse(
            s"/pillar2/subscription/$plrReference",
            OK
          )
          val result = await(connector.getSubscriptionInformation(plrReference))
          result.status mustBe OK
        }
      }

      "must return status as NOT_FOUND" in {

        forAll(plrReferenceGen) { (plrReference: String) =>
          stubGetResponse(
            s"/pillar2/subscription/$plrReference",
            NOT_FOUND
          )

          val result = connector.getSubscriptionInformation(plrReference).futureValue
          result.status mustBe NOT_FOUND
        }
      }

      "must return status as BAD_REQUEST" in {

        forAll(plrReferenceGen) { (plrReference: String) =>
          stubGetResponse(
            s"/pillar2/subscription/$plrReference",
            BAD_REQUEST
          )

          val result = connector.getSubscriptionInformation(plrReference).futureValue
          result.status mustBe BAD_REQUEST
        }
      }

      "must return status as INTERNAL_SERVER_ERROR" in {

        forAll(plrReferenceGen) { (plrReference: String) =>
          stubGetResponse(
            s"/pillar2/subscription/$plrReference",
            INTERNAL_SERVER_ERROR
          )

          val result = connector.getSubscriptionInformation(plrReference).futureValue
          result.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "amendSubscriptionInformation" - {

      "must return status as OK for a successful amendment" in {
        forAll(arbitrary[AmendSubscriptionInput]) { amendRequest =>
          stubPutResponse(
            s"/pillar2/subscription",
            OK
          )

          val result = await(connector.amendSubscriptionInformation(amendRequest))
          result.status mustBe OK
        }
      }

      "should handle 400 Bad Request" in {
        forAll { amendRequest: AmendSubscriptionInput =>
          stubPutResponse("/pillar2/subscription", BAD_REQUEST)

          val result = await(connector.amendSubscriptionInformation(amendRequest))

          result.status mustBe BAD_REQUEST
        }
      }

      "should handle exceptions" in {
        forAll { amendRequest: AmendSubscriptionInput =>
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
