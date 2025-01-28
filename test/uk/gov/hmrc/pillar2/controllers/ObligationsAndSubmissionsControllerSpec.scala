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

package uk.gov.hmrc.pillar2.controllers

import org.mockito.Mockito.reset
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.service.ObligationsAndSubmissionsService

class ObligationsAndSubmissionsControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction],
      bind[ObligationsAndSubmissionsService].to(mockObligationsAndSubmissionsService)
    )
    .build()

  override def afterEach(): Unit = {
    reset(mockObligationsAndSubmissionsService)
    reset(mockAuthConnector)
    super.afterEach()
  }

  "ObligationsAndSubmissionsController" - {

//    "should return OK with obligations and submissions when data is found for plrReference" in {
//        ??? //TODO: How do we specify plrReference as the only query params seem to be date related
//    }
  }

}
