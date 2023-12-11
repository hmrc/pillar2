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

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.pillar2.models.grs._
import uk.gov.hmrc.pillar2.models.hods._
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.models.registration._
import uk.gov.hmrc.pillar2.models.subscription.{ExtraSubscription, SubscriptionAddress, SubscriptionRequestParameters}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, NonUKAddress, RowStatus, UKAddress, UserAnswers}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.pillar2.models.subscription.ReadSubscriptionRequestParameters
import java.time.{Instant, LocalDate}

trait ModelGenerators {
  self: Generators =>

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1))
  }

  implicit val arbitraryRegistration: Arbitrary[RegisterWithoutIDRequest] = Arbitrary {
    for {
      ack            <- arbitrary[String]
      name           <- arbitrary[String]
      address        <- arbitrary[Address]
      contactDetails <- arbitrary[ContactDetails]
      identification <- Gen.option(arbitrary[Identification])
    } yield RegisterWithoutIDRequest(
      regime = "PLR",
      acknowledgementReference = ack,
      isAnAgent = false,
      isAGroup = true,
      identification = identification,
      NoIdOrganisation(name),
      address = address,
      contactDetails = contactDetails
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

  //---------------------------------------------------

  val arbitraryAnyIdUpeFmUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id <- nonEmptyString
      userData <- oneOf(
                    Seq(
                      arbitraryWithoutIdUpeFmUserData.arbitrary,
                      arbitraryWithIdUpeFmUserData.arbitrary,
                      arbitraryWithIdUpeAndNoIdFmUserData.arbitrary,
                      arbitraryWithoutIdUpeAndWithIdFmUserData.arbitrary,
                      arbitraryWithIdUpeAndNoNominatedFilingMember.arbitrary,
                      arbitraryWithoutIdUpeNoNominatedFilingMember.arbitrary
                    )
                  )
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryWithoutIdUpeFmUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitraryWithoutIdUpeFmUserData.arbitrary

    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryUncompleteUpeFmUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitraryUncompleteUpeFmUserData.arbitrary
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  val arbitraryWithoutIdUpeFmUserData: Arbitrary[JsValue] = Arbitrary {
    for {

      upeNameRegistration      <- stringsWithMaxLength(105)
      upeRegisteredAddress     <- arbitraryUKAddressDetails.arbitrary
      upeContactName           <- stringsWithMaxLength(200)
      upeContactEmail          <- arbitrary[String]
      upeCapturePhone          <- arbitrary[Int]
      fmNameRegistration       <- stringsWithMaxLength(200)
      fmRegisteredAddress      <- arbitraryNonUKAddressDetails.arbitrary
      fmContactName            <- stringsWithMaxLength(200)
      fmContactEmail           <- stringsWithMaxLength(200)
      fmCapturePhone           <- arbitrary[Int]
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> false,
      "upeNameRegistration"         -> upeNameRegistration,
      "upeRegisteredAddress"        -> upeRegisteredAddress,
      "upeContactName"              -> upeContactName,
      "upeContactEmail"             -> upeContactEmail,
      "upeCapturePhone"             -> upeCapturePhone,
      "NominateFilingMember"        -> true,
      "fmRegisteredInUK"            -> false,
      "fmNameRegistration"          -> fmNameRegistration,
      "fmRegisteredAddress"         -> fmRegisteredAddress,
      "fmContactName"               -> fmContactName,
      "fmContactEmail"              -> fmContactEmail,
      "fmPhonePreference"           -> true,
      "fmCapturePhone"              -> fmCapturePhone,
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryWithIdUpeFmUserData: Arbitrary[JsValue] = Arbitrary {
    for {

      upeGRSResponse           <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
      fmGRSResponse            <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> true,
      "GrsUpStatus"                 -> RowStatus.Completed.value,
      "upeEntityType"               -> "ukLimitedCompany",
      "upeGRSResponse"              -> upeGRSResponse,
      "fmGRSResponse"               -> fmGRSResponse,
      "NominateFilingMember"        -> true,
      "fmRegisteredInUK"            -> true,
      "GrsFilingMemberStatus"       -> RowStatus.Completed.value,
      "fmEntityType"                -> "ukLimitedCompany",
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryWithIdUpeAndNoIdFmUserData: Arbitrary[JsValue] = Arbitrary {
    for {

      upeGRSResponse           <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
      fmNameRegistration       <- stringsWithMaxLength(200)
      fmRegisteredAddress      <- arbitraryNonUKAddressDetails.arbitrary
      fmContactName            <- stringsWithMaxLength(200)
      fmContactEmail           <- stringsWithMaxLength(200)
      fmCapturePhone           <- arbitrary[Int]
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> true,
      "GrsUpStatus"                 -> RowStatus.Completed.value,
      "upeEntityType"               -> "ukLimitedCompany",
      "upeGRSResponse"              -> upeGRSResponse,
      "NominateFilingMember"        -> true,
      "fmRegisteredInUK"            -> false,
      "fmNameRegistration"          -> fmNameRegistration,
      "fmRegisteredAddress"         -> fmRegisteredAddress,
      "fmContactName"               -> fmContactName,
      "fmContactEmail"              -> fmContactEmail,
      "fmPhonePreference"           -> true,
      "fmCapturePhone"              -> fmCapturePhone,
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryWithoutIdUpeAndWithIdFmUserData: Arbitrary[JsValue] = Arbitrary {
    for {

      upeNameRegistration      <- stringsWithMaxLength(105)
      upeRegisteredAddress     <- arbitraryUKAddressDetails.arbitrary
      upeContactName           <- stringsWithMaxLength(200)
      upeContactEmail          <- arbitrary[String]
      upeCapturePhone          <- arbitrary[Int]
      fmGRSResponse            <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> false,
      "upeNameRegistration"         -> upeNameRegistration,
      "upeRegisteredAddress"        -> upeRegisteredAddress,
      "upeContactName"              -> upeContactName,
      "upeContactEmail"             -> upeContactEmail,
      "upeCapturePhone"             -> upeCapturePhone,
      "fmGRSResponse"               -> fmGRSResponse,
      "NominateFilingMember"        -> true,
      "fmRegisteredInUK"            -> true,
      "GrsFilingMemberStatus"       -> RowStatus.Completed.value,
      "fmEntityType"                -> "ukLimitedCompany",
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryWithIdUpeAndNoNominatedFilingMember: Arbitrary[JsValue] = Arbitrary {
    for {

      upeGRSResponse           <- arbitraryWithIdRegDataForLimitedCompany.arbitrary
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> true,
      "GrsUpStatus"                 -> RowStatus.Completed.value,
      "upeEntityType"               -> "ukLimitedCompany",
      "upeGRSResponse"              -> upeGRSResponse,
      "NominateFilingMember"        -> false,
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryWithoutIdUpeNoNominatedFilingMember: Arbitrary[JsValue] = Arbitrary {
    for {

      upeNameRegistration      <- stringsWithMaxLength(105)
      subAccountingPeriod      <- arbitrary[AccountingPeriod]
      subPrimaryEmail          <- arbitrary[String]
      subPrimaryContactName    <- stringsWithMaxLength(200)
      subSecondaryContactName  <- stringsWithMaxLength(200)
      subSecondaryEmail        <- arbitrary[String]
      subSecondaryCapturePhone <- arbitrary[Int]
      subRegisteredAddress     <- arbitraryNonUKAddressDetails.arbitrary

    } yield Json.obj(
      "upeRegisteredInUK"           -> false,
      "upeNameRegistration"         -> upeNameRegistration,
      "NominateFilingMember"        -> false,
      "subMneOrDomestic"            -> "ukAndOther",
      "subAccountingPeriod"         -> subAccountingPeriod,
      "subUsePrimaryContact"        -> true,
      "subPrimaryEmail"             -> subPrimaryEmail,
      "subPrimaryPhonePreference"   -> true,
      "subPrimaryContactName"       -> subPrimaryContactName,
      "subAddSecondaryContact"      -> true,
      "subSecondaryContactName"     -> subSecondaryContactName,
      "subSecondaryEmail"           -> subSecondaryEmail,
      "subSecondaryPhonePreference" -> true,
      "subSecondaryCapturePhone"    -> subSecondaryCapturePhone,
      "subRegisteredAddress"        -> subRegisteredAddress
    )
  }

  val arbitraryUncompleteUpeFmUserData: Arbitrary[JsValue] = Arbitrary {
    for {

      upeNameRegistration  <- stringsWithMaxLength(105)
      upeRegisteredAddress <- arbitraryUKAddressDetails.arbitrary
      upeContactName       <- stringsWithMaxLength(200)
      upeContactEmail      <- arbitrary[String]
      upeCapturePhone      <- arbitrary[Int]
      fmNameRegistration   <- stringsWithMaxLength(200)
      fmRegisteredAddress  <- arbitraryNonUKAddressDetails.arbitrary
      fmContactName        <- stringsWithMaxLength(200)
      fmContactEmail       <- stringsWithMaxLength(200)
      fmCapturePhone       <- arbitrary[Int]

    } yield Json.obj(
      "upeRegisteredInUK"    -> false,
      "upeNameRegistration"  -> upeNameRegistration,
      "upeRegisteredAddress" -> upeRegisteredAddress,
      "upeContactName"       -> upeContactName,
      "upeContactEmail"      -> upeContactEmail,
      "upeCapturePhone"      -> upeCapturePhone,
      "NominateFilingMember" -> true,
      "fmRegisteredInUK"     -> false,
      "fmNameRegistration"   -> fmNameRegistration,
      "fmRegisteredAddress"  -> fmRegisteredAddress,
      "fmContactName"        -> fmContactName,
      "fmContactEmail"       -> fmContactEmail,
      "fmPhonePreference"    -> true,
      "fmCapturePhone"       -> fmCapturePhone,
      "subMneOrDomestic"     -> "ukAndOther"
    )
  }

  implicit val arbitraryUKAddressDetails: Arbitrary[UKAddress] = Arbitrary {

    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postCode     <- arbitrary[String]
      countryCode  <- arbitrary[String]
    } yield UKAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postCode,
      countryCode = countryCode
    )
  }

  implicit val arbitraryNonUKAddressDetails: Arbitrary[NonUKAddress] = Arbitrary {

    for {
      addressLine1 <- arbitrary[String]
      addressLine2 <- Gen.option(arbitrary[String])
      addressLine3 <- arbitrary[String]
      addressLine4 <- Gen.option(arbitrary[String])
      postCode     <- Gen.option(arbitrary[String])
      countryCode  <- arbitrary[String]
    } yield NonUKAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postalCode = postCode,
      countryCode = countryCode
    )
  }

  //----------------------------------------------------

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
      safeId = Some(safeId),
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

  implicit val arbitraryFilingMemberAmendDetails: Arbitrary[FilingMemberAmendDetails] = Arbitrary {
    for {
      addNewFm                <- arbitrary[Boolean]
      safeId                  <- arbitrary[String]
      customerIdentification1 <- Gen.option(arbitrary[String])
      customerIdentification2 <- Gen.option(arbitrary[String])
      organisationName        <- arbitrary[String]
    } yield FilingMemberAmendDetails(
      addNewFm,
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

  implicit val arbitraryAccountStatus: Arbitrary[AccountStatus] = Arbitrary {
    arbitrary[Boolean].map(AccountStatus(_))
  }

  implicit val arbitrarySubscriptionResponse: Arbitrary[SubscriptionResponse] = Arbitrary {
    for {
      success <- arbitrary[SubscriptionSuccess]
    } yield SubscriptionResponse(success)
  }

  implicit val arbitrarySubscriptionSuccess: Arbitrary[SubscriptionSuccess] = Arbitrary {
    for {
      formBundleNumber         <- stringsWithMaxLength(20)
      upeDetails               <- arbitrary[UpeDetails]
      upeCorrespAddressDetails <- arbitrary[UpeCorrespAddressDetails]
      primaryContactDetails    <- arbitrary[ContactDetailsType]
      secondaryContactDetails  <- Gen.option(arbitrary[ContactDetailsType])
      filingMemberDetails      <- Gen.option(arbitrary[FilingMemberDetails])
      accountingPeriod         <- arbitrary[AccountingPeriod]
      accountStatus            <- Gen.option(arbitrary[AccountStatus])
    } yield SubscriptionSuccess(
      formBundleNumber,
      upeDetails,
      upeCorrespAddressDetails,
      primaryContactDetails,
      secondaryContactDetails,
      filingMemberDetails,
      accountingPeriod,
      accountStatus
    )
  }

  implicit val readSubscriptionRequestParametersArbitrary: Arbitrary[ReadSubscriptionRequestParameters] = Arbitrary {
    for {
      id           <- Gen.alphaStr
      plrReference <- Gen.alphaStr
    } yield ReadSubscriptionRequestParameters(id, plrReference)
  }

  implicit val arbitraryUpeDetailsAmend: Arbitrary[UpeDetailsAmend] = Arbitrary {
    for {
      plrReference            <- arbitrary[String]
      customerIdentification1 <- Gen.option(arbitrary[String])
      customerIdentification2 <- Gen.option(arbitrary[String])
      organisationName        <- arbitrary[String]
      registrationDate        <- arbitrary[LocalDate]
      domesticOnly            <- arbitrary[Boolean]
      filingMember            <- arbitrary[Boolean]
    } yield UpeDetailsAmend(
      plrReference = plrReference,
      customerIdentification1 = customerIdentification1,
      customerIdentification2 = customerIdentification2,
      organisationName = organisationName,
      registrationDate = registrationDate,
      domesticOnly = domesticOnly,
      filingMember = filingMember
    )
  }
  implicit val arbitraryAmendSubscriptionSuccess: Arbitrary[AmendSubscriptionSuccess] = Arbitrary {
    for {
      upeDetails               <- arbitrary[UpeDetailsAmend]
      accountingPeriod         <- arbitrary[AccountingPeriod]
      upeCorrespAddressDetails <- arbitrary[UpeCorrespAddressDetails]
      primaryContactDetails    <- arbitrary[ContactDetailsType]
      secondaryContactDetails  <- Gen.option(arbitrary[ContactDetailsType])
      filingMemberDetails      <- Gen.option(arbitrary[FilingMemberAmendDetails])
    } yield AmendSubscriptionSuccess(
      upeDetails,
      accountingPeriod,
      upeCorrespAddressDetails,
      primaryContactDetails,
      secondaryContactDetails,
      filingMemberDetails
    )
  }

  implicit val arbitraryAmendSubscriptionInput: Arbitrary[AmendSubscriptionInput] = Arbitrary {
    arbitraryAmendSubscriptionSuccess.arbitrary.map(AmendSubscriptionInput(_))
  }

  implicit val arbitraryExtraSubscription: Arbitrary[ExtraSubscription] = Arbitrary {
    for {
      formBundleNumber <- arbitrary[String]
      crn              <- arbitrary[String]
      utr              <- arbitrary[String]
      safeId           <- arbitrary[String]
      plrReference     <- arbitrary[String]
    } yield ExtraSubscription(
      Some(formBundleNumber),
      Some(crn),
      Some(utr),
      Some(safeId),
      Some(plrReference)
    )
  }

  val arbitraryAmendSubscriptionUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      plrRef               <- stringsWithMaxLength(20)
      id                   <- Gen.uuid.map(_.toString)
      upeNameRegistration  <- stringsWithMaxLength(105)
      primaryContactName   <- stringsWithMaxLength(200)
      primaryEmail         <- stringsWithMaxLength(20)
      secondaryContactName <- stringsWithMaxLength(200)
      secondaryEmail       <- stringsWithMaxLength(20)
      secondaryPhone       <- arbitrary[Int]
      filingMemberSafeId   <- stringsWithMaxLength(200)
      registrationDate     <- arbitrary[LocalDate]
      subRegisteredAddress <- arbitraryNonUKAddressDetails.arbitrary

      filingMember      <- arbitraryFilingMemberAmendDetails.arbitrary
      acountPeriod      <- arbitraryAccountingPeriod.arbitrary
      extraSubscription <- arbitraryExtraSubscription.arbitrary
      data = Json.obj(
               "plrReference"                -> plrRef,
               "subMneOrDomestic"            -> "ukAndOther",
               "upeNameRegistration"         -> upeNameRegistration,
               "subPrimaryContactName"       -> primaryContactName,
               "subPrimaryEmail"             -> primaryEmail,
               "subSecondaryContactName"     -> secondaryContactName,
               "subSecondaryEmail"           -> secondaryEmail,
               "subSecondaryCapturePhone"    -> secondaryPhone,
               "FmSafeID"                    -> filingMemberSafeId,
               "subFilingMemberDetails"      -> filingMember,
               "subAccountingPeriod"         -> acountPeriod,
               "subRegistrationDate"         -> registrationDate,
               "subPrimaryCapturePhone"      -> secondaryPhone,
               "subPrimaryPhonePreference"   -> true,
               "subSecondaryPhonePreference" -> true,
               "subAddSecondaryContact"      -> true,
               "subExtraSubscription"        -> extraSubscription,
               "subRegisteredAddress"        -> subRegisteredAddress,
               "NominateFilingMember"        -> true
             )
    } yield UserAnswers(id, data, Instant.now)
  }

  val arbitraryIncompleteAmendSubscriptionUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      plrRef               <- stringsWithMaxLength(20)
      id                   <- Gen.uuid.map(_.toString)
      upeNameRegistration  <- stringsWithMaxLength(105)
      primaryContactName   <- stringsWithMaxLength(200)
      primaryEmail         <- stringsWithMaxLength(20)
      secondaryContactName <- stringsWithMaxLength(200)
      registrationDate     <- arbitrary[LocalDate]
      subRegisteredAddress <- arbitraryNonUKAddressDetails.arbitrary
      acountPeriod         <- arbitraryAccountingPeriod.arbitrary
      data = Json.obj(
               "plrReference"                -> plrRef,
               "subMneOrDomestic"            -> "ukAndOther",
               "upeNameRegistration"         -> upeNameRegistration,
               "subPrimaryContactName"       -> primaryContactName,
               "subPrimaryEmail"             -> primaryEmail,
               "subAccountingPeriod"         -> acountPeriod,
               "subRegistrationDate"         -> registrationDate,
               "subPrimaryPhonePreference"   -> true,
               "subSecondaryPhonePreference" -> true,
               "subAddSecondaryContact"      -> false,
               "subRegisteredAddress"        -> subRegisteredAddress,
               "NominateFilingMember"        -> false
             )
    } yield UserAnswers(id, data, Instant.now)
  }

}
