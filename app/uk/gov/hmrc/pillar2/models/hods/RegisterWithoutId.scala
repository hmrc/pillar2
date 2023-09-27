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

package uk.gov.hmrc.pillar2.models.hods

import play.api.libs.json._
import uk.gov.hmrc.pillar2.models.fm.NfmRegisteredAddress
import uk.gov.hmrc.pillar2.models.registration.UpeRegisteredAddress

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

case class NoIdOrganisation(organisationName: String)

object NoIdOrganisation {

  implicit val format: OFormat[NoIdOrganisation] = Json.format[NoIdOrganisation]

}

case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: String,
  addressLine4: Option[String],
  postalCode:   Option[String],
  countryCode:  String
)

object Address {
  implicit val addressFormat: OFormat[Address] = Json.format[Address]

  def fromAddress(address: UpeRegisteredAddress): Address =
    Address(address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, address.postalCode, address.countryCode)

  def fromFmAddress(address: NfmRegisteredAddress): Address =
    Address(address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, address.postalCode, address.countryCode)

}

case class ContactDetails(
  phoneNumber:  Option[String],
  mobileNumber: Option[String],
  faxNumber:    Option[String],
  emailAddress: Option[String]
)

object ContactDetails {
  implicit val contactFormats = Json.format[ContactDetails]
}

case class Identification(
  idNumber:           String,
  issuingInstitution: String,
  issuingCountryCode: String
)

object Identification {
  implicit val indentifierFormats = Json.format[Identification]
}

case class RequestParameter(paramName: String, paramValue: String)

object RequestParameter {
  implicit val indentifierFormats = Json.format[RequestParameter]
}

case class RequestCommon(
  receiptDate:              String,
  regime:                   String,
  acknowledgementReference: String,
  requestParameters:        Option[Seq[RequestParameter]]
)

object RequestCommon {
  implicit val requestCommonFormats: OFormat[RequestCommon] = Json.format[RequestCommon]

  def apply(regime: String): RequestCommon = {
    val acknRef: String = UUID.randomUUID().toString.replaceAll("-", "") //uuids are 36 and spec demands 32
    //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val dateTime: String = ZonedDateTime
      .now(ZoneId.of("UTC"))
      .format(formatter)
    RequestCommon(dateTime, regime, acknRef, None)
  }
}

case class RequestDetails(
  organisation:   NoIdOrganisation,
  address:        Address,
  contactDetails: ContactDetails,
  identification: Option[Identification]
)

object RequestDetails {

  implicit lazy val residentWrites = Json.writes[RequestDetails]

  implicit lazy val reads: Reads[RequestDetails] = {
    import play.api.libs.functional.syntax._
    (
      (__ \ "organisation").read[NoIdOrganisation] and
        (__ \ "address").read[Address] and
        (__ \ "contactDetails").read[ContactDetails] and
        (__ \ "identification").readNullable[Identification]
    )((organisation, address, contactDetails, identification) => RequestDetails(organisation, address, contactDetails, identification))
  }
}

case class RegisterWithoutIDRequest(
  requestCommon: RequestCommon,
  requestDetail: RequestDetails
)

object RegisterWithoutIDRequest {
  implicit val format = Json.format[RegisterWithoutIDRequest]
}

case class RegisterWithoutId(
  registerWithoutIDRequest: RegisterWithoutIDRequest
)

object RegisterWithoutId {
  implicit val format: OFormat[RegisterWithoutId] = Json.format[RegisterWithoutId]

  def apply(organisationName: String, address: Address, contactDetails: ContactDetails): RegisterWithoutId =
    RegisterWithoutId(
      RegisterWithoutIDRequest(
        RequestCommon("PILLAR2"),
        RequestDetails(NoIdOrganisation(organisationName), address, contactDetails, None)
      )
    )
}
