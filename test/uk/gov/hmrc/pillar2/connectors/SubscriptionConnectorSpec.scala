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
import play.api.test.Helpers.await
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.request.CreateSubscriptionRequest

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

        forAll(arbitrary[CreateSubscriptionRequest]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            OK
          )
          val result = await(connector.sendCreateSubscriptionInformation(sub))
          result.status mustBe OK
        }
      }

      "must return status as BAD_REQUEST" in {

        forAll(arbitrary[CreateSubscriptionRequest]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            BAD_REQUEST
          )

          val result = connector.sendCreateSubscriptionInformation(sub).futureValue
          result.status mustBe BAD_REQUEST
        }
      }

      "must return status as INTERNAL_SERVER_ERROR" in {

        forAll(arbitrary[CreateSubscriptionRequest]) { sub =>
          stubResponse(
            "/pillar2/subscription",
            INTERNAL_SERVER_ERROR
          )

          val result = connector.sendCreateSubscriptionInformation(sub).futureValue
          result.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

  }

}