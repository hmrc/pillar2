/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.models.errors

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.pillar2.models.errors.Pillar2Error.*

class Pillar2ErrorSpec extends AnyFreeSpec with Matchers {

  "Pillar2Error.getMessage must override the Throwable.getMessage and" - {

    "return the custom detail message for MissingHeaderError" in {
      val error = MissingHeaderError("X-Test-Header")
      error.getMessage mustBe "Code: '001' Message: 'Missing X-Test-Header header'"
    }

    "return the custom detail message for InvalidJsonError" in {
      val error = InvalidJsonError("{\"invalidKey\": \"value\"}")
      error.getMessage mustBe "Code: '002' Message: 'Invalid JSON payload: {\"invalidKey\": \"value\"}'"
    }

    "return the custom detail message for ApiInternalServerError" in {
      val error = ApiInternalServerError
      error.getMessage mustBe "Code: '003' Message: 'An unexpected error occurred'"
    }

    "return the custom detail message for AuthorizationError" in {
      val error = AuthorizationError
      error.getMessage mustBe "Code: '401' Message: 'Not Authorized'"
    }

    "return the custom detail message for SubscriptionProcessingError" in {
      val error = SubscriptionProcessingError
      error.getMessage mustBe "Code: '422' Message: 'Subscription is being processed'"
    }

    "return the custom detail message for ETMPValidationError" in {
      val error = ETMPValidationError("089", "ID number missing or invalid")
      error.getMessage mustBe "Code: '089' Message: 'ID number missing or invalid'"
    }
  }

  "Pillar2Error.ETMPValidationError must" - {
    "not fill in the stack trace" in {
      val error = ETMPValidationError("014", "No data found")
      error.getStackTrace mustBe empty
    }
  }

}
