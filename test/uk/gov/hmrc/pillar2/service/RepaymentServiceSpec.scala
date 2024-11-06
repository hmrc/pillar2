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

package uk.gov.hmrc.pillar2.service

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail

import scala.concurrent.Future

class RepaymentServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val service =
    new RepaymentService(
      mockRepaymentConnector
    )

  "RepaymentService" - {
    "Return Done in case of a CREATED Http Response" in {
      forAll(arbitraryRepaymentPayload.arbitrary) { repaymentPayLoad =>
        when(mockRepaymentConnector.sendRepaymentDetails(any[RepaymentRequestDetail])(any[HeaderCarrier]()))
          .thenReturn(Future.successful(HttpResponse(status = 201, json = JsObject.empty, Map.empty)))
        val result = service.sendRepaymentsData(repaymentPayLoad)
        result.futureValue mustBe Done
      }
    }
    "return a failed result in case of a response other than 201" in {
      forAll(arbitraryRepaymentPayload.arbitrary) { repaymentPayLoad =>
        when(mockRepaymentConnector.sendRepaymentDetails(any[RepaymentRequestDetail])(any[HeaderCarrier]()))
          .thenReturn(Future.successful(HttpResponse(status = 200, json = JsObject.empty, Map.empty)))
        val result = service.sendRepaymentsData(repaymentPayLoad)
        result.failed.futureValue mustBe UnexpectedResponse
      }
    }
  }

}
