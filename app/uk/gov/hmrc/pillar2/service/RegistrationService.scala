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

import cats.syntax.flatMap._
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.RegistrationConnector
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.audit.{NominatedFilingMember, UpeRegistration}
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, RegisterWithoutIDRequest}
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.service.audit.AuditService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject() (
  dataSubmissionConnectors: RegistrationConnector,
  auditService:             AuditService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendNoIdUpeRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      upeName      <- userAnswers.get(upeNameRegistrationId)
      emailAddress <- userAnswers.get(upeContactEmailId)
      address      <- userAnswers.get(upeRegisteredAddressId)
    } yield registerWithoutId(
      upeName,
      Address.fromAddress(address),
      ContactDetails(userAnswers.get(upeCapturePhoneId), None, None, Some(emailAddress)),
      false
    )
  }.getOrElse {
    logger.warn("Ultimate Parent registration failed as one or more required fields were missing")
    registerWithoutIdError
  }

  def sendNoIdFmRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      fmName       <- userAnswers.get(fmNameRegistrationId)
      emailAddress <- userAnswers.get(fmContactEmailId)
      address      <- userAnswers.get(fmRegisteredAddressId)

    } yield registerWithoutId(
      fmName,
      Address.fromFmAddress(address),
      ContactDetails(userAnswers.get(fmCapturePhoneId), None, None, Some(emailAddress)),
      true
    )
  }.getOrElse {
    logger.warn("Filing member registration failed as one or more required fields were missing")
    registerWithoutIdError
  }

  def registerNewFilingMember(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    (for {
      name    <- userAnswers.get(RfmNameRegistrationId)
      email   <- userAnswers.get(RfmPrimaryContactEmailId)
      address <- userAnswers.get(RfmRegisteredAddressId)
    } yield registerWithoutId(
      name,
      Address.fromFmAddress(address),
      ContactDetails(userAnswers.get(RfmPrimaryPhoneId), None, None, Some(email)),
      isFm = true
    )).getOrElse {
      logger.warn("Replace Filing member registration failed as one or more required fields were missing")
      registerWithoutIdError
    }

  private def registerWithoutId(businessName: String, address: Address, contactDetails: ContactDetails, isFm: Boolean)(implicit
    hc:                                       HeaderCarrier
  ): Future[HttpResponse] = {
    val registerWithoutIDRequest = RegisterWithoutIDRequest(businessName, address, contactDetails)
    dataSubmissionConnectors
      .sendWithoutIDInformation(registerWithoutIDRequest)(hc, ec)
      .flatTap { _ =>
        if (isFm) {
          val auditData = convertNfmAuditDetails(registerWithoutIDRequest)
          auditService.auditFmRegisterWithoutId(auditData)
        } else {
          val auditData = convertUpeAuditDetails(registerWithoutIDRequest)
          auditService.auditUpeRegisterWithoutId(auditData)
        }
      }
  }

  private def convertUpeAuditDetails(registerWithoutIDRequest: RegisterWithoutIDRequest): UpeRegistration =
    UpeRegistration(
      registeredinUK = true,
      entityType = "not Listed",
      ultimateParentEntityName = registerWithoutIDRequest.organisation.organisationName,
      addressLine1 = registerWithoutIDRequest.address.addressLine1,
      addressLine2 = registerWithoutIDRequest.address.addressLine2.getOrElse(""),
      townOrCity = registerWithoutIDRequest.address.addressLine3,
      region = registerWithoutIDRequest.address.addressLine4.getOrElse(""),
      postCode = registerWithoutIDRequest.address.postalCode.getOrElse(""),
      country = registerWithoutIDRequest.address.countryCode,
      name = "",
      email = registerWithoutIDRequest.contactDetails.emailAddress.getOrElse(""),
      telephoneNo = registerWithoutIDRequest.contactDetails.phoneNumber.getOrElse("")
    )

  private def convertNfmAuditDetails(registerWithoutIDRequest: RegisterWithoutIDRequest): NominatedFilingMember =
    NominatedFilingMember(
      registerNomFilingMember = true,
      registeredinUK = true,
      nominatedFilingMemberName = registerWithoutIDRequest.organisation.organisationName,
      addressLine1 = registerWithoutIDRequest.address.addressLine1,
      addressLine2 = registerWithoutIDRequest.address.addressLine2.getOrElse(""),
      townOrCity = registerWithoutIDRequest.address.addressLine3,
      region = registerWithoutIDRequest.address.addressLine4.getOrElse(""),
      postCode = registerWithoutIDRequest.address.postalCode.getOrElse(""),
      country = registerWithoutIDRequest.address.countryCode,
      name = "",
      email = registerWithoutIDRequest.contactDetails.emailAddress.getOrElse(""),
      telephoneNo = registerWithoutIDRequest.contactDetails.phoneNumber.getOrElse("")
    )

  private val registerWithoutIdError =
    Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "RegistrationService - Response not received in registration"))

}
