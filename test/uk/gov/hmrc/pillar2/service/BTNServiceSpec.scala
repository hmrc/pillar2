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

package uk.gov.hmrc.pillar2.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.btn.BTNRequest

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class BTNServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service = new BTNService(mockBTNConnector)

  private val btnPayload =
    BTNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1)
    )

  "sendBtn" - {
    "should return the connector response as is" in {
        val httpResponse = HttpResponse(200, "{}")
        when(mockBTNConnector.sendBtn(any[BTNRequest])(using any[HeaderCarrier](), any[String]()))
          .thenReturn(Future.successful(httpResponse))

        val result = service.sendBtn(btnPayload).futureValue
        result mustBe httpResponse
    }
  }
}
