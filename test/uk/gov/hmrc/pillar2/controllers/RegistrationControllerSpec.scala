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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.RegistrationService

import scala.concurrent.{ExecutionContext, Future}

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
  val jsData:      JsValue     = Json.parse("""{"value": "field"}""")
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
  def routeFmWithoutID(id:  String): String = routes.RegistrationController.withoutIdFmRegistrationSubmission(id).url
  def rfmRoute(id:          String): String = routes.RegistrationController.registerNewFilingMember(id).url

  "withoutIdUpeRegistrationSubmission" - {

    "return OK with valid data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "return BAD_REQUEST with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        val errorBody = Json
          .obj(
            "errorDetail" -> Json.obj(
              "timestamp"         -> "2026-07-24T10:00:00Z",
              "correlationId"     -> "correlation-id",
              "errorCode"         -> "400",
              "errorMessage"      -> "Bad Request",
              "source"            -> "ETMP",
              "sourceFaultDetail" -> Json.obj(
                "detail" -> Json.arr("Invalid registration")
              )
            )
          )
          .toString()

        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, errorBody)))

        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "return NOT_FOUND with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }

    "return FORBIDDEN with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "return INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, routeUpeWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

    "throw an unexpected exception when submitting no-ID UPE registration" in new Setup {
      when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(jsData)))

      when(mockDataSubmissionsService.sendNoIdUpeRegistration(any[UserAnswers]())(using any[HeaderCarrier]()))
        .thenReturn(Future.failed(new RuntimeException("someError")))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routeUpeWithoutID("id")).withJsonBody(jsData)
      val result:  Throwable                     = route(application, request).value.failed.futureValue

      result.getMessage mustEqual "someError"
    }

  }

  "withoutIdFmRegistrationSubmission" - {

    "return OK with valid data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "return BAD_REQUEST with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(BAD_REQUEST, "Bad Request")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST

      }
    }

    "return NOT_FOUND with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }

    "return FORBIDDEN with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "return INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(arbitraryWithoutIdUpeFmUserAnswers.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, routeFmWithoutID(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

    "throw an unexpected exception when submitting no-ID FM registration" in new Setup {
      when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(jsData)))

      when(mockDataSubmissionsService.sendNoIdFmRegistration(any[UserAnswers]())(using any[HeaderCarrier]()))
        .thenReturn(Future.failed(new RuntimeException("someError")))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routeFmWithoutID("id")).withJsonBody(jsData)
      val result:  Throwable                     = route(application, request).value.failed.futureValue

      result.getMessage mustEqual "someError"
    }

  }

  "RegisterNewFilingMember" - {

    "return OK with valid data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(OK, "Success")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual OK

      }
    }

    "return BAD_REQUEST with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(BAD_REQUEST, "Bad Request")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST

      }
    }

    "return NOT_FOUND with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(NOT_FOUND, "Not Found")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }

    "return FORBIDDEN with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(FORBIDDEN, "Forbidden")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual FORBIDDEN

      }
    }

    "return INTERNAL_SERVER_ERROR with data has been submitted" in new Setup {
      forAll(NewFilingMemberRegistrationDetails.arbitrary) { userAnswers =>
        when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]())).thenReturn(Future.successful(Some(jsData)))
        when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]())).thenReturn(
          Future.successful(
            HttpResponse.apply(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
          )
        )
        val request = FakeRequest(POST, rfmRoute(userAnswers.id)).withJsonBody(jsData)
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

    "throw an unexpected exception when registering a new filing member" in new Setup {
      when(mockRegistrationCacheRepository.get(any[String]())(using any[ExecutionContext]()))
        .thenReturn(Future.successful(Some(jsData)))

      when(mockDataSubmissionsService.registerNewFilingMember(any[UserAnswers]())(using any[HeaderCarrier]()))
        .thenReturn(Future.failed(new RuntimeException("someError")))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, rfmRoute("id")).withJsonBody(jsData)
      val result:  Throwable                     = route(application, request).value.failed.futureValue

      result.getMessage mustEqual "someError"
    }

  }

}
