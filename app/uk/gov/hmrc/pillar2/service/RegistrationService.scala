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

import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Reads
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.RegistrationConnector
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.audit.{NominatedFilingMember, UpeRegistration}
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, RegisterWithoutIDRequest}
import uk.gov.hmrc.pillar2.models.{NonUKAddress, UKAddress}
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.queries.Gettable
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
    val upeName      = userAnswers.get(upeNameRegistrationId.gettable).getOrElse("Unknown UPE Name")
    val emailAddress = userAnswers.get(upeContactEmailId.gettable).getOrElse("unknown@example.com")
    val phone        = userAnswers.get(upeCapturePhoneId.gettable).getOrElse("0000000000")
    val address      = extractAddress[UKAddress](userAnswers, upeRegisteredAddressId.gettable)

    registerWithoutId(
      upeName,
      address,
      ContactDetails(Some(phone), None, None, Some(emailAddress)),
      isFm = false
    )
  }

  def sendNoIdFmRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val fmName       = userAnswers.get(fmNameRegistrationId.gettable).getOrElse("Unknown FM Name")
    val emailAddress = userAnswers.get(fmContactEmailId.gettable).getOrElse("unknownfm@example.com")
    val phone        = userAnswers.get(fmCapturePhoneId.gettable).getOrElse("0000000000")
    val address      = extractAddress[NonUKAddress](userAnswers, fmRegisteredAddressId.gettable)

    registerWithoutId(
      fmName,
      address,
      ContactDetails(Some(phone), None, None, Some(emailAddress)),
      isFm = true
    )
  }

  def registerNewFilingMember(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val name    = userAnswers.get(RfmNameRegistrationId.gettable).getOrElse("Unknown Filing Member")
    val email   = userAnswers.get(RfmPrimaryContactEmailId.gettable).getOrElse("unknownfm@example.com")
    val phone   = userAnswers.get(RfmPrimaryPhoneId.gettable).getOrElse("0000000000")
    val address = extractAddress[NonUKAddress](userAnswers, RfmRegisteredAddressId.gettable)

    registerWithoutId(
      name,
      address,
      ContactDetails(Some(phone), None, None, Some(email)),
      isFm = true
    )
  }

  private def extractAddress[T](userAnswers: UserAnswers, addressId: Gettable[T])(implicit reads: Reads[T]): Address = {
    val addressPath = addressId.path
    userAnswers.get(addressId) match {
      case Some(ukAddress: UKAddress) =>
        Address.fromAddress(ukAddress)
      case Some(nonUKAddress: NonUKAddress) =>
        Address.fromFmAddress(nonUKAddress)
      case _ =>
        Address("Unknown Line 1", Some(""), "Unknown City", Some(""), Some(""), "UK")
    }
  }
  private def registerWithoutId(businessName: String, address: Address, contactDetails: ContactDetails, isFm: Boolean)(implicit
    hc:                                       HeaderCarrier
  ): Future[HttpResponse] = {
    val registerWithoutIDRequest = RegisterWithoutIDRequest(businessName, address, contactDetails)
    val response = dataSubmissionConnectors
      .sendWithoutIDInformation(registerWithoutIDRequest)(hc, ec)
    response.map { _ =>
      if (isFm) {
        val auditData = convertNfmAuditDetails(registerWithoutIDRequest)
        auditService.auditFmRegisterWithoutId(auditData)
      } else {
        val auditData = convertUpeAuditDetails(registerWithoutIDRequest)
        auditService.auditUpeRegisterWithoutId(auditData)
      }
    }
    response
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
