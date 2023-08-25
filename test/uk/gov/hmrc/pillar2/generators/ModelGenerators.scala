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
import uk.gov.hmrc.pillar2.models.{RowStatus, UserAnswers, YesNoType}
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, Identification, NoIdOrganisation, RegisterWithoutIDRequest, RegisterWithoutId, RequestCommon, RequestDetails}
import uk.gov.hmrc.pillar2.models.registration.{Registration, UpeRegisteredAddress, UserData, WithoutIdRegData}

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

  implicit val arbitraryUserAnswers: Arbitrary[UserAnswers] = Arbitrary {
    for {
      id       <- nonEmptyString
      userData <- arbitrary[UserData]
    } yield UserAnswers(
      id = id,
      data = Json.toJson(userData).as[JsObject],
      lastUpdated = Instant.now
    )
  }

  implicit val arbitraryUserData: Arbitrary[UserData] = Arbitrary {
    for {
      regisrtation <- arbitrary[Registration]
    } yield UserData(regisrtation, None)
  }

  implicit val arbitraryUserAnswerRegistration: Arbitrary[Registration] = Arbitrary {

    for {
      withoutIdRegData <- arbitrary[WithoutIdRegData]
    } yield Registration(
      isUPERegisteredInUK = YesNoType.No,
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
      Some(YesNoType.No)
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
}
