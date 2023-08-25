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

package uk.gov.hmrc.pillar2.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.service.DataSubmissionsService

import scala.concurrent.Future
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.models.UserAnswers

class DataSubmissionsServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val controller =
      new DataSubmissionsService(
        mockRgistrationCacheRepository,
        mockDataSubmissionsConnector
      )
  }

  "sendBusinessRegistration" - {
    "Return successful Http Response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any())(any(), any())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(OK, "Success")
        )
      )

      forAll(arbitrary[UserAnswers]) { userAnswers =>
        controller.sendBusinessRegistration(userAnswers).map { response =>
          response.status mustBe OK
        }
      }

    }

    "Return internal server error with response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any())(any(), any())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")
        )
      )

      forAll(arbitrary[UserAnswers]) { userAnswers =>
        controller.sendBusinessRegistration(userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }

}
