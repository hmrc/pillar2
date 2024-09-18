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

package uk.gov.hmrc.pillar2.models.hods

import play.api.libs.json._
import uk.gov.hmrc.pillar2.models.{NonUKAddress, UKAddress}

import java.util.UUID

final case class NoIdOrganisation(organisationName: String)

object NoIdOrganisation {
  implicit val format: OFormat[NoIdOrganisation] = Json.format[NoIdOrganisation]
}

final case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: String,
  addressLine4: Option[String],
  postalCode:   Option[String],
  countryCode:  String
)

object Address {
  implicit val addressFormat: OFormat[Address] = Json.format[Address]

  def fromAddress(address: UKAddress): Address =
    Address(address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, Some(address.postalCode), address.countryCode)

  def fromFmAddress(address: NonUKAddress): Address =
    Address(address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, address.postalCode, address.countryCode)
}

final case class ContactDetails(
  phoneNumber:  Option[String],
  mobileNumber: Option[String],
  faxNumber:    Option[String],
  emailAddress: Option[String]
)

object ContactDetails {
  implicit val contactFormats: OFormat[ContactDetails] = Json.format[ContactDetails] // Explicit type added
}

final case class Identification(
  idNumber:           String,
  issuingInstitution: String,
  issuingCountryCode: String
)

object Identification {
  implicit val indentifierFormats: OFormat[Identification] = Json.format[Identification] // Explicit type added
}

final case class RegisterWithoutIDRequest(
  regime:                   String,
  acknowledgementReference: String,
  isAnAgent:                Boolean,
  isAGroup:                 Boolean,
  identification:           Option[Identification],
  organisation:             NoIdOrganisation,
  address:                  Address,
  contactDetails:           ContactDetails
)

object RegisterWithoutIDRequest {
  implicit val format: OFormat[RegisterWithoutIDRequest] = Json.format[RegisterWithoutIDRequest] // Explicit type added

  def apply(organisationName: String, address: Address, contactDetails: ContactDetails): RegisterWithoutIDRequest =
    RegisterWithoutIDRequest(
      regime = "PLR",
      acknowledgementReference = UUID.randomUUID().toString.replaceAll("-", ""), // UUIDs are 36 characters, spec demands 32
      isAnAgent = false,
      isAGroup = true,
      identification = None,
      organisation = NoIdOrganisation(organisationName),
      address = address,
      contactDetails
    )
}
