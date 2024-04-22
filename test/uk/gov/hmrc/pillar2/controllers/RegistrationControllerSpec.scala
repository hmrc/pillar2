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

package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val controller =
      new RegistrationController(
        mockRegistrationCacheRepository,
        mockDataSubmissionsService,
        mockAuthAction,
        stubControllerComponents()
      )
  }
  val jsData = Json.parse("""{"value": "field"}""")
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRegistrationCacheRepository),
      bind[RegistrationService].toInstance(mockDataSubmissionsService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  def routeUpeWithoutID(id: String): String = routes.RegistrationController.withoutIdUpeRegistrationSubmission(id).url
  def routeFmWithoutID(id: String):  String = routes.RegistrationController.withoutIdFmRegistrationSubmission(id).url
  def rfmRoute(id: String):          String = routes.RegistrationController.registerNewFilingMember(id).url

  "withoutIdUpeRegistrationSubmission" - {

    "rerutn OK with valid data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "rerutn BAD_REQUEST with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(BAD_REQUEST, "Bad Request")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST

      }
    }

    "rerutn NOT_FOUND with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }
    "rerutn FORBIDDEN with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "rerutn INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

  }

  "withoutIdFmRegistrationSubmission" - {

    "rerutn OK with valid data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "rerutn BAD_REQUEST with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(BAD_REQUEST, "Bad Request")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST

      }
    }

    "rerutn NOT_FOUND with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }
    "rerutn FORBIDDEN with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "rerutn INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

  }

  "RegisterNewFilingMember" - {

    "rerutn OK with valid data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "rerutn BAD_REQUEST with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(BAD_REQUEST, "Bad Request")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST

      }
    }

    "rerutn NOT_FOUND with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }
    "rerutn FORBIDDEN with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "rerutn INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any())(any())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

  }

}
