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

package uk.gov.hmrc.pillar2.generators

import java.time.{Instant, LocalDate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.pillar2.models.fm.{FilingMember, NfmRegisteredAddress, WithoutIdNfmData}
import uk.gov.hmrc.pillar2.models.grs.{BusinessVerificationResult, EntityType, GrsRegistrationResult, RegistrationStatus, VerificationStatus}
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{ContactDetailsType, FilingMemberDetails, UpeCorrespAddressDetails, UpeDetails}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.{CreateSubscriptionRequest, RequestCommonForSubscription, RequestDetail, RequestParameters, SubscriptionRequest}
import uk.gov.hmrc.pillar2.models.{AccountingPeriod, RowStatus, UserAnswers}
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, Identification, NoIdOrganisation, RegisterWithoutIDRequest, RegisterWithoutId, RequestCommon, RequestDetails}
import uk.gov.hmrc.pillar2.models.registration.{CompanyProfile, GrsResponse, IncorporatedEntityAddress, IncorporatedEntityRegistrationData, PartnershipEntityRegistrationData, Registration, UpeRegisteredAddress, UserData, WithoutIdRegData}
import uk.gov.hmrc.pillar2.models.subscription.{MneOrDomestic, Subscription, SubscriptionAddress, SubscriptionRequestParameters}

trait ModelGenerators {
  self: Generators =>

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1))
  }

  implicit val arbitraryRequestCommon: Arbitrary[RequestCommon] = Arbitrary {
    for {
      receiptDate        <- arbitrary[String]
      acknowledgementRef <- stringsWithMaxLength(32)

    } yield RequestCommon(
      receiptDate = receiptDate,
      regime = "PIL2",
      acknowledgementReference = acknowledgementRef,
      None
    )
  }

  implicit val arbitraryRegistration: Arbitrary[RegisterWithoutId] = Arbitrary {
    for {
      requestCommon  <- arbitrary[RequestCommon]
      name           <- arbitrary[String]
      address        <- arbitrary[Address]
      contactDetails <- arbitrary[ContactDetails]
      identification <- Gen.option(arbitrary[Identification])
    } yield RegisterWithoutId(
      RegisterWithoutIDRequest(
        requestCommon,
        RequestDetails(
          NoIdOrganisation(name),
          address = address,
          contactDetails = contactDetails,
          identification = identification
        )
      )
    )
  }

  implicit val arbitraryAddress: Arbitrary[Address] = Arbitrary {
    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postalCode   <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield Address(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postalCode,
      countryCode = countryCode
    )
  }

  implicit val arbitraryContactDetails: Arbitrary[ContactDetails] = Arbitrary {
    for {
      phoneNumber  <- Gen.option(arbitrary[String])
      mobileNumber <- Gen.option(arbitrary[String])
      faxNumber    <- Gen.option(arbitrary[String])
      emailAddress <- Gen.option(arbitrary[String])
    } yield ContactDetails(
      phoneNumber = phoneNumber,
      mobileNumber = mobileNumber,
      faxNumber = faxNumber,
      emailAddress = emailAddress
    )
  }

  implicit val arbitraryIdentification: Arbitrary[Identification] = Arbitrary {
    for {
      idNumber           <- arbitrary[String]
      issuingInstitution <- arbitrary[String]
      issuingCountryCode <- arbitrary[String]
    } yield Identification(
      idNumber = idNumber,
      issuingInstitution = issuingInstitution,
      issuingCountryCode = issuingCountryCode
    )
  }

  //NO-ID USER ANSWERS
  val arbitraryNoIdUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitraryNoIdUserData.arbitrary
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryNoIdUserData: Arbitrary[UserData] = Arbitrary {
    for {
      regisrtation <- arbitraryNoIdRegistration.arbitrary
      filingMember <- arbitraryNoIdFilingMember.arbitrary
      subscription <- arbitrary[Subscription]
    } yield UserData(regisrtation, Some(filingMember), Some(subscription))
  }

  val arbitraryNoIdRegistration: Arbitrary[Registration] = Arbitrary {

    for {
      withoutIdRegData <- arbitrary[WithoutIdRegData]
    } yield Registration(
      isUPERegisteredInUK = false,
      isRegistrationStatus = RowStatus.InProgress,
      withoutIdRegData = Some(withoutIdRegData)
    )
  }

  implicit val arbitraryWithoutIdRegData: Arbitrary[WithoutIdRegData] = Arbitrary {
    for {
      upeNameRegistration  <- arbitrary[String]
      upeRegisteredAddress <- arbitrary[UpeRegisteredAddress]
      upeContactName       <- arbitrary[String]
      emailAddress         <- arbitrary[String]

    } yield WithoutIdRegData(
      upeNameRegistration,
      Some(upeRegisteredAddress),
      Some(upeContactName),
      Some(emailAddress),
      Some(false)
    )
  }

  implicit val arbitraryUpeRegisteredAddress: Arbitrary[UpeRegisteredAddress] = Arbitrary {
    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postalCode   <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield UpeRegisteredAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postalCode,
      countryCode = countryCode
    )
  }

  val arbitraryNoIdFilingMember: Arbitrary[FilingMember] = Arbitrary {

    for {
      withoutIdNfmData <- arbitrary[WithoutIdNfmData]
    } yield FilingMember(
      nfmConfirmation = true,
      isNfmRegisteredInUK = Some(false),
      isNFMnStatus = RowStatus.InProgress,
      withoutIdRegData = Some(withoutIdNfmData)
    )
  }

  implicit val arbitraryWithoutIdNfmData: Arbitrary[WithoutIdNfmData] = Arbitrary {
    for {
      upeNameRegistration  <- arbitrary[String]
      upeRegisteredAddress <- arbitrary[NfmRegisteredAddress]
      upeContactName       <- arbitrary[String]
      emailAddress         <- arbitrary[String]

    } yield WithoutIdNfmData(
      upeNameRegistration,
      Some(upeRegisteredAddress),
      Some(upeContactName),
      Some(emailAddress),
      Some(false)
    )
  }

  implicit val arbitraryNfmRegisteredAddress: Arbitrary[NfmRegisteredAddress] = Arbitrary {
    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postalCode   <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield NfmRegisteredAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postalCode,
      countryCode = countryCode
    )
  }

  //UNCOMPLETE USER ANSWER - ERROR
  val arbitraryAnyIdUncompletedUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitraryAnyIdUncompletedUserData.arbitrary
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryAnyIdUncompletedUserData: Arbitrary[UserData] = Arbitrary {
    for {
      regisrtation <-
        oneOf(
          Seq(arbitraryWithIdRegistrationForLimitedCompany.arbitrary, arbitraryWithIdRegistrationFoLLP.arbitrary, arbitraryNoIdRegistration.arbitrary)
        )
      filingMember <-
        oneOf(
          Seq(arbitraryWithIdFilingMemberForLimitedCompany.arbitrary, arbitraryWithIdFilingMemberFoLLP.arbitrary, arbitraryNoIdFilingMember.arbitrary)
        )
    } yield UserData(regisrtation, Some(filingMember), None)
  }

  //ANYID USER ANSWERS
  val arbitraryAnyIdUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitraryAnyIdUserData.arbitrary
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryAnyIdUserData: Arbitrary[UserData] = Arbitrary {
    for {
      regisrtation <-
        oneOf(
          Seq(arbitraryWithIdRegistrationForLimitedCompany.arbitrary, arbitraryWithIdRegistrationFoLLP.arbitrary, arbitraryNoIdRegistration.arbitrary)
        )
      filingMember <-
        oneOf(
          Seq(arbitraryWithIdFilingMemberForLimitedCompany.arbitrary, arbitraryWithIdFilingMemberFoLLP.arbitrary, arbitraryNoIdFilingMember.arbitrary)
        )
      subscription <- arbitrary[Subscription]
    } yield UserData(regisrtation, Some(filingMember), Some(subscription))
  }

  val arbitraryWithIdRegistrationForLimitedCompany: Arbitrary[Registration] = Arbitrary {

    for {
      withIdRegData <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
    } yield Registration(
      isUPERegisteredInUK = true,
      orgType = Some(EntityType.UKLimitedCompany),
      isRegistrationStatus = RowStatus.InProgress,
      withIdRegData = Some(withIdRegData)
    )
  }

  val arbitraryWithIdRegistrationFoLLP: Arbitrary[Registration] = Arbitrary {

    for {
      withIdRegData <- arbitraryWithIdRegDataFoLLP.arbitrary
    } yield Registration(
      isUPERegisteredInUK = true,
      orgType = Some(EntityType.LimitedLiabilityPartnership),
      isRegistrationStatus = RowStatus.InProgress,
      withIdRegData = Some(withIdRegData)
    )
  }

  val arbitraryWithIdFilingMemberForLimitedCompany: Arbitrary[FilingMember] = Arbitrary {
    for {
      withIdRegData <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
    } yield FilingMember(
      nfmConfirmation = true,
      isNfmRegisteredInUK = Some(true),
      orgType = Some(EntityType.UKLimitedCompany),
      isNFMnStatus = RowStatus.Completed,
      withIdRegData = Some(withIdRegData)
    )
  }

  val arbitraryWithIdFilingMemberFoLLP: Arbitrary[FilingMember] = Arbitrary {

    for {
      withIdRegData <- arbitraryWithIdRegDataFoLLP.arbitrary
    } yield FilingMember(
      nfmConfirmation = true,
      isNfmRegisteredInUK = Some(true),
      orgType = Some(EntityType.LimitedLiabilityPartnership),
      isNFMnStatus = RowStatus.Completed,
      withIdRegData = Some(withIdRegData)
    )
  }

  val arbitraryWithIdRegDataForLimitedCompany: Arbitrary[GrsResponse] = Arbitrary {
    for {
      incorporatedEntityRegistrationData <- arbitrary[IncorporatedEntityRegistrationData]

    } yield GrsResponse(incorporatedEntityRegistrationData = Some(incorporatedEntityRegistrationData))
  }

  val arbitraryWithIdRegDataFoLLP: Arbitrary[GrsResponse] = Arbitrary {
    for {
      partnershipEntityRegistrationData <- arbitrary[PartnershipEntityRegistrationData]

    } yield GrsResponse(partnershipEntityRegistrationData = Some(partnershipEntityRegistrationData))
  }

  implicit val arbitraryIncorporatedEntityRegistrationData: Arbitrary[IncorporatedEntityRegistrationData] = Arbitrary {

    for {
      companyProfile <- arbitrary[CompanyProfile]
      ctutr          <- arbitrary[String]
    } yield IncorporatedEntityRegistrationData(
      companyProfile = companyProfile,
      ctutr = ctutr,
      identifiersMatch = true,
      businessVerification = Some(BusinessVerificationResult(verificationStatus = VerificationStatus.Pass)),
      registration =
        GrsRegistrationResult(registrationStatus = RegistrationStatus.Registered, registeredBusinessPartnerId = Some("XB0000000000001"), None)
    )
  }

  implicit val arbitraryPartnershipEntityRegistrationData: Arbitrary[PartnershipEntityRegistrationData] = Arbitrary {

    for {
      companyProfile <- arbitrary[CompanyProfile]
      sautr          <- arbitrary[String]
      postCode       <- arbitrary[String]
    } yield PartnershipEntityRegistrationData(
      companyProfile = Some(companyProfile),
      sautr = Some(sautr),
      postcode = Some(postCode),
      identifiersMatch = true,
      businessVerification = Some(BusinessVerificationResult(verificationStatus = VerificationStatus.Pass)),
      registration =
        GrsRegistrationResult(registrationStatus = RegistrationStatus.Registered, registeredBusinessPartnerId = Some("XB0000000000001"), None)
    )
  }

  implicit val arbitraryCompanyProfile: Arbitrary[CompanyProfile] = Arbitrary {

    for {
      companyName            <- arbitrary[String]
      companyNumber          <- arbitrary[String]
      dateOfIncorporation    <- arbitrary[LocalDate]
      unsanitisedCHROAddress <- arbitrary[IncorporatedEntityAddress]

    } yield CompanyProfile(
      companyName = companyName,
      companyNumber = companyNumber,
      dateOfIncorporation = dateOfIncorporation,
      unsanitisedCHROAddress = unsanitisedCHROAddress
    )
  }

  implicit val arbitraryIncorporatedEntityAddress: Arbitrary[IncorporatedEntityAddress] = Arbitrary {
    for {
      address_line_1 <- arbitrary[String]
      address_line_2 <- Gen.option(arbitrary[String])
      country        <- arbitrary[String]
      locality       <- Gen.option(arbitrary[String])
      po_box         <- Gen.option(arbitrary[String])
      postal_code    <- Gen.option(arbitrary[String])
      premises       <- Gen.option(arbitrary[String])
      region         <- Gen.option(arbitrary[String])

    } yield IncorporatedEntityAddress(
      address_line_1 = Some(address_line_1),
      address_line_2 = address_line_2,
      country = Some(country),
      locality = locality,
      po_box = po_box,
      postal_code = postal_code,
      premises = premises,
      region = region
    )
  }

  //SUBSCRIPTION DATA
  implicit val arbitrarySubscription: Arbitrary[Subscription] = Arbitrary {
    for {
      accountingPeriod          <- arbitrary[AccountingPeriod]
      primaryContactName        <- Gen.option(arbitrary[String])
      primaryContactEmail       <- Gen.option(arbitrary[String])
      primaryContactTelephone   <- Gen.option(arbitrary[String])
      secondaryContactName      <- Gen.option(arbitrary[String])
      secondaryContactEmail     <- Gen.option(arbitrary[String])
      secondaryContactTelephone <- Gen.option(arbitrary[String])
      correspondenceAddress     <- arbitrary[SubscriptionAddress]
    } yield Subscription(
      domesticOrMne = MneOrDomestic.uk,
      groupDetailStatus = RowStatus.Completed,
      accountingPeriod = Some(accountingPeriod),
      primaryContactName = primaryContactName,
      primaryContactEmail = primaryContactEmail,
      primaryContactTelephone = primaryContactTelephone,
      secondaryContactName = secondaryContactName,
      secondaryContactEmail = secondaryContactEmail,
      secondaryContactTelephone = secondaryContactTelephone,
      correspondenceAddress = Some(correspondenceAddress)
    )
  }

  implicit val arbitrarySubscriptionAddress: Arbitrary[SubscriptionAddress] = Arbitrary {
    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postalCode   <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield SubscriptionAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postalCode,
      countryCode = countryCode
    )
  }

  implicit val arbitraryCreateSubscriptionRequest: Arbitrary[CreateSubscriptionRequest] = Arbitrary {
    for {
      createSubscriptionRequest <- arbitrary[SubscriptionRequest]
    } yield CreateSubscriptionRequest(
      createSubscriptionRequest = createSubscriptionRequest
    )
  }

  implicit val arbitrarySubscriptionRequest: Arbitrary[SubscriptionRequest] = Arbitrary {
    for {
      requestCommon <- arbitrary[RequestCommonForSubscription]
      requestDetail <- arbitrary[RequestDetail]
    } yield SubscriptionRequest(
      requestCommon = requestCommon,
      requestDetail = requestDetail
    )
  }

  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] = Arbitrary {
    for {
      receiptDate              <- arbitrary[String]
      acknowledgementReference <- arbitrary[String]

    } yield RequestCommonForSubscription(
      regime = "PIL2",
      conversationID = None,
      receiptDate = receiptDate,
      acknowledgementReference = acknowledgementReference,
      originatingSystem = "MDTP",
      None
    )
  }

  implicit val arbitraryRequestDetail: Arbitrary[RequestDetail] = Arbitrary {
    for {
      upeDetails               <- arbitrary[UpeDetails]
      accountingPeriod         <- arbitrary[AccountingPeriod]
      upeCorrespAddressDetails <- arbitrary[UpeCorrespAddressDetails]
      primaryContactDetails    <- arbitrary[ContactDetailsType]
      secondaryContactDetails  <- Gen.option(arbitrary[ContactDetailsType])
      filingMemberDetails      <- Gen.option(arbitrary[FilingMemberDetails])
    } yield RequestDetail(
      upeDetails = upeDetails,
      accountingPeriod = accountingPeriod,
      upeCorrespAddressDetails = upeCorrespAddressDetails,
      primaryContactDetails = primaryContactDetails,
      secondaryContactDetails = secondaryContactDetails,
      filingMemberDetails = filingMemberDetails
    )
  }

  implicit val arbitraryUpeDetails: Arbitrary[UpeDetails] = Arbitrary {
    for {
      safeId                  <- arbitrary[String]
      customerIdentification1 <- Gen.option(arbitrary[String])
      customerIdentification2 <- Gen.option(arbitrary[String])
      organisationName        <- arbitrary[String]
      registrationDate        <- arbitrary[LocalDate]
      domesticOnly            <- arbitrary[Boolean]
      filingMember            <- arbitrary[Boolean]
    } yield UpeDetails(
      safeId = safeId,
      customerIdentification1 = customerIdentification1,
      customerIdentification2 = customerIdentification2,
      organisationName = organisationName,
      registrationDate = registrationDate,
      domesticOnly = domesticOnly,
      filingMember = filingMember
    )
  }

  implicit val arbitraryAccountingPeriod: Arbitrary[AccountingPeriod] = Arbitrary {
    for {
      startDate <- arbitrary[LocalDate]
      endDate   <- arbitrary[LocalDate]
    } yield AccountingPeriod(
      startDate = startDate,
      endDate = endDate
    )
  }

  implicit val arbitraryUpeCorrespAddressDetails: Arbitrary[UpeCorrespAddressDetails] = Arbitrary {

    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- Gen.option(arbitrary[String])
      addressLine4 <- Gen.option(arbitrary[String])
      postCode     <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield UpeCorrespAddressDetails(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postCode = postCode,
      countryCode = countryCode
    )
  }

  implicit val arbitraryContactDetailsType: Arbitrary[ContactDetailsType] = Arbitrary {
    for {
      name         <- arbitrary[String]
      telephone    <- Gen.option(arbitrary[String])
      emailAddress <- arbitrary[String]
    } yield ContactDetailsType(
      name = name,
      telephone = telephone,
      emailAddress = emailAddress
    )
  }

  implicit val arbitraryFilingMemberDetails: Arbitrary[FilingMemberDetails] = Arbitrary {
    for {
      safeId                  <- arbitrary[String]
      customerIdentification1 <- Gen.option(arbitrary[String])
      customerIdentification2 <- Gen.option(arbitrary[String])
      organisationName        <- arbitrary[String]
    } yield FilingMemberDetails(
      safeId = safeId,
      customerIdentification1 = customerIdentification1,
      customerIdentification2 = customerIdentification2,
      organisationName = organisationName
    )
  }

  implicit val arbitrarySubscriptionRequestParameters: Arbitrary[SubscriptionRequestParameters] = Arbitrary {
    for {
      id        <- arbitrary[String]
      regSafeId <- arbitrary[String]
      fmSafeId  <- Gen.option(arbitrary[String])

    } yield SubscriptionRequestParameters(
      id = id,
      regSafeId = regSafeId,
      fmSafeId = fmSafeId
    )
  }

}
