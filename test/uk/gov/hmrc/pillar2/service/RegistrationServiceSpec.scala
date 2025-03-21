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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.RegisterWithoutIDRequest
import uk.gov.hmrc.pillar2.service.RegistrationService

import scala.concurrent.{ExecutionContext, Future}

class RegistrationServiceSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val service =
      new RegistrationService(
        mockDataSubmissionsConnector,
        mockAuditService
      )
  }

  "sendNoIdUpeRegistration" - {
    "Return successful Http Response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(OK, "Success")
        )
      )

      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        service.sendNoIdUpeRegistration(userAnswers).map { response =>
          response.status mustBe OK
        }
      }

    }

    "Return internal server error with response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")
        )
      )

      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        service.sendNoIdUpeRegistration(userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }

  "sendNoIdFmRegistration" - {
    "Return successful Http Response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(OK, "Success")
        )
      )

      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        service.sendNoIdFmRegistration(userAnswers).map { response =>
          response.status mustBe OK
        }
      }

    }

    "Return internal server error with response" in new Setup {
      when(
        mockDataSubmissionsConnector
          .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
      ).thenReturn(
        Future.successful(
          HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")
        )
      )

      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        service.sendNoIdFmRegistration(userAnswers).map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    "registerNewFilingMember" - {
      "Return successful Http Response" in new Setup {
        when(
          mockDataSubmissionsConnector
            .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )

        forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
          service.registerNewFilingMember(userAnswers).map { response =>
            response.status mustBe OK
          }
        }

      }

      "Return internal server error with response" in new Setup {
        when(
          mockDataSubmissionsConnector
            .sendWithoutIDInformation(any[RegisterWithoutIDRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "Internal Server Error")
          )
        )

        forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
          service.registerNewFilingMember(userAnswers).map { response =>
            response.status mustBe INTERNAL_SERVER_ERROR
          }
        }

      }
    }
  }

}
