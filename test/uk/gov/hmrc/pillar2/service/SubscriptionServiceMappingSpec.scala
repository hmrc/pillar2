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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.hods.subscription.common.*
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceMappingSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks with ScalaFutures {
  private val mockedCache = mock[ReadSubscriptionCacheRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockedCache)
    reset(mockSubscriptionConnector)
    reset(mockAuditService)
  }

  private val service = new SubscriptionService(mockedCache, mockSubscriptionConnector, mockAuditService)

  "storeSubscriptionResponse" - {
    "must correctly map contact details to ReadSubscriptionCachedData" in {
      val plrReference = "XMPLR0012345678"
      val id           = "testId"

      // Create a SubscriptionSuccess object with known contact details
      val subscriptionSuccess = arbitrarySubscriptionSuccess.arbitrary.sample.value.copy(
        primaryContactDetails = ContactDetailsType(
          name = "Test Name",
          telephone = Some("0123456789"),
          emailAddress = "test@example.com"
        ),
        secondaryContactDetails = Some(
          ContactDetailsType(
            name = "Secondary Name",
            telephone = Some("0987654321"),
            emailAddress = "secondary@example.com"
          )
        )
      )

      val subscriptionResponse = SubscriptionResponse(subscriptionSuccess)

      when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(subscriptionResponse), Map.empty)))

      when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionResponse]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))

      when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.unit)

      service.storeSubscriptionResponse(id, plrReference).futureValue

      val captor = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockedCache).upsert(eqTo(id), captor.capture())(using any[ExecutionContext]())

      val capturedData = captor.getValue.as[ReadSubscriptionCachedData]

      // Verify Primary Contact Mapping
      capturedData.subPrimaryContactName mustBe "Test Name"
      capturedData.subPrimaryEmail mustBe "test@example.com"
      capturedData.subPrimaryCapturePhone mustBe Some("0123456789")
      capturedData.subPrimaryPhonePreference mustBe true

      // Verify Secondary Contact Mapping
      capturedData.subSecondaryContactName mustBe Some("Secondary Name")
      capturedData.subSecondaryEmail mustBe Some("secondary@example.com")
      capturedData.subSecondaryCapturePhone mustBe Some("0987654321")
      capturedData.subSecondaryPhonePreference mustBe Some(true)
    }

    "must correctly map contact details when telephone is missing" in {
      val plrReference = "XMPLR0012345678"
      val id           = "testId"

      // Create a SubscriptionSuccess object with known contact details
      val subscriptionSuccess = arbitrarySubscriptionSuccess.arbitrary.sample.value.copy(
        primaryContactDetails = ContactDetailsType(
          name = "Test Name",
          telephone = None,
          emailAddress = "test@example.com"
        ),
        secondaryContactDetails = Some(
          ContactDetailsType(
            name = "Secondary Name",
            telephone = None,
            emailAddress = "secondary@example.com"
          )
        )
      )

      val subscriptionResponse = SubscriptionResponse(subscriptionSuccess)

      when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(subscriptionResponse), Map.empty)))

      when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionResponse]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))

      when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.unit)

      service.storeSubscriptionResponse(id, plrReference).futureValue

      val captor = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockedCache).upsert(eqTo(id), captor.capture())(using any[ExecutionContext]())

      val capturedData = captor.getValue.as[ReadSubscriptionCachedData]

      // Verify Primary Contact Mapping
      capturedData.subPrimaryContactName mustBe "Test Name"
      capturedData.subPrimaryEmail mustBe "test@example.com"
      capturedData.subPrimaryCapturePhone mustBe None
      capturedData.subPrimaryPhonePreference mustBe false

      // Verify Secondary Contact Mapping
      capturedData.subSecondaryContactName mustBe Some("Secondary Name")
      capturedData.subSecondaryEmail mustBe Some("secondary@example.com")
      capturedData.subSecondaryCapturePhone mustBe None
      capturedData.subSecondaryPhonePreference mustBe None
    }

    "must correctly map contact details when phone field is used instead of telephone" in {
      val plrReference = "XMPLR0012345678"
      val id           = "testId"

      // Construct JSON manually to use "phone" instead of "telephone"
      val primaryJson = Json.obj(
        "name"         -> "Test Name",
        "phone"        -> "0123456789",
        "emailAddress" -> "test@example.com"
      )

      val secondaryJson = Json.obj(
        "name"         -> "Secondary Name",
        "phone"        -> "0987654321",
        "emailAddress" -> "secondary@example.com"
      )

      val baseSuccess = arbitrarySubscriptionSuccess.arbitrary.sample.value
      val subscriptionResponseJson = Json.obj(
        "success" -> Json.obj(
          "formBundleNumber"         -> "bundle1",
          "upeDetails"               -> Json.toJson(baseSuccess.upeDetails),
          "upeCorrespAddressDetails" -> Json.toJson(baseSuccess.upeCorrespAddressDetails),
          "primaryContactDetails"    -> primaryJson,
          "secondaryContactDetails"  -> secondaryJson,
          "filingMemberDetails"      -> Json.toJson(baseSuccess.filingMemberDetails),
          "accountingPeriod"         -> Json.toJson(baseSuccess.accountingPeriod),
          "accountStatus"            -> Json.toJson(baseSuccess.accountStatus)
        )
      )

      when(mockSubscriptionConnector.getSubscriptionInformation(any[String]())(using any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(200, subscriptionResponseJson, Map.empty)))

      when(mockAuditService.auditReadSubscriptionSuccess(any[String](), any[SubscriptionResponse]())(using any[HeaderCarrier]()))
        .thenReturn(Future.successful(AuditResult.Success))

      when(mockedCache.upsert(any[String](), any[JsValue]())(using any[ExecutionContext]())).thenReturn(Future.unit)

      service.storeSubscriptionResponse(id, plrReference).futureValue

      val captor = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockedCache).upsert(eqTo(id), captor.capture())(using any[ExecutionContext]())

      val capturedData = captor.getValue.as[ReadSubscriptionCachedData]

      // Verify Primary Contact Mapping from 'phone'
      capturedData.subPrimaryCapturePhone mustBe Some("0123456789")
      capturedData.subPrimaryPhonePreference mustBe true

      // Verify Secondary Contact Mapping from 'phone'
      capturedData.subSecondaryCapturePhone mustBe Some("0987654321")
      capturedData.subSecondaryPhonePreference mustBe Some(true)
    }
  }
}
