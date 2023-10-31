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

import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.SubscriptionConnector
import uk.gov.hmrc.pillar2.models.fm.FilingMember
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.{CreateSubscriptionRequest, RequestDetail, SubscriptionRequest}
import uk.gov.hmrc.pillar2.models.identifiers.{EntityTypeId, FilingMemberId, NominateFilingMemberId, RegistrationId, SubscriptionId, fmEntityTypeId, fmGRSResponseId, fmNameRegistrationId, fmRegisteredInUKId, subAccountingPeriodId, subMneOrDomesticId, subPrimaryCapturePhoneId, subPrimaryContactNameId, subPrimaryEmailId, subRegisteredAddressId, subSecondaryCapturePhoneId, subSecondaryContactNameId, subSecondaryEmailId, upeEntityTypeId, upeGRSResponseId, upeNameRegistrationId, upeRegisteredInUKId}
import uk.gov.hmrc.pillar2.models.registration.{GrsResponse, Registration}
import uk.gov.hmrc.pillar2.models.subscription.{MneOrDomestic, Subscription}
import uk.gov.hmrc.pillar2.models.{AccountingPeriod, NonUKAddress, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (
  repository:             RegistrationCacheRepository,
  subscriptionConnectors: SubscriptionConnector,
  countryOptions:         CountryOptions
)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendCreateSubscription(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                 HeaderCarrier
  ): Future[HttpResponse] = {
    for {
      upeOrgType          <- userAnswers.get(upeEntityTypeId)
      upeRegisteredInUK   <- userAnswers.get(upeRegisteredInUKId)
      subMneOrDomestic    <- userAnswers.get(subMneOrDomesticId)
      nominateFm          <- userAnswers.get(NominateFilingMemberId)
      upeGrsResponse      <- userAnswers.get(upeGRSResponseId)
      upeNameRegistration <- userAnswers.get(upeNameRegistrationId)
      fmRegisteredInUK    <- userAnswers.get(fmRegisteredInUKId)
      fmEntityTypeId      <- userAnswers.get(fmEntityTypeId)
      fmGrsResponseId     <- userAnswers.get(fmGRSResponseId)
      fmNameRegistration  <- userAnswers.get(fmNameRegistrationId)
      subAddressId        <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod    <- userAnswers.get(subAccountingPeriodId)

      primaryContactName <- userAnswers.get(subPrimaryContactNameId)
      primaryPhone       <- userAnswers.get(subPrimaryCapturePhoneId)
      primaryEmail       <- userAnswers.get(subPrimaryEmailId)

      secondaryContactName <- userAnswers.get(subSecondaryContactNameId)
      secondaryEmail       <- userAnswers.get(subSecondaryEmailId)
      secondaryPhone       <- userAnswers.get(subSecondaryCapturePhoneId)

    } yield {
      val subscriptionRequest = CreateSubscriptionRequest(
        createSubscriptionRequest = SubscriptionRequest(
          requestBody = RequestDetail(
            getUpeDetails(upeSafeId, upeOrgType, upeRegisteredInUK, subMneOrDomestic, nominateFm, upeGrsResponse, upeNameRegistration),
            getAccountingPeriod(accountingPeriod),
            getUpeAddressDetails(subAddressId),
            getPrimaryContactDetails(primaryContactName, primaryPhone, primaryEmail),
            Some(getSecondaryContactDetails(secondaryContactName, secondaryEmail, secondaryPhone)),
            getFilingMemberDetails(fmSafeId, fmRegisteredInUK, nominateFm, fmEntityTypeId, fmGrsResponseId, fmNameRegistration)
          )
        )
      )
      sendSubmissionRequest(subscriptionRequest)
    }
  }.getOrElse {
    logger.warn("Subscription Information Missing")
    subscriptionError
  }

  private def sendSubmissionRequest(subscriptionRequest: CreateSubscriptionRequest)(implicit
    hc:                                                  HeaderCarrier,
    ec:                                                  ExecutionContext
  ): Future[HttpResponse] =
    subscriptionConnectors
      .sendCreateSubscriptionInformation(subscriptionRequest)(hc, ec)

  private val subscriptionError = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription"))

  private def getUpeDetails(
    upeSafeId:           String,
    upeOrgType:          EntityType,
    upeRegisteredInUK:   Boolean,
    subMneOrDomestic:    MneOrDomestic,
    nominateFm:          Boolean,
    upeGrsResponse:      GrsResponse,
    upeNameRegistration: String
  ): UpeDetails = {
    val domesticOnly = if (subMneOrDomestic == MneOrDomestic.uk) true else false
    upeRegisteredInUK match {
      case true =>
        upeOrgType match {
          case EntityType.UKLimitedCompany =>
            val incorporatedEntityRegistrationData =
              upeGrsResponse.incorporatedEntityRegistrationData.getOrElse(throw new Exception("Malformed Incorporation Registration Data"))
            val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
            val name = incorporatedEntityRegistrationData.companyProfile.companyName
            val utr  = incorporatedEntityRegistrationData.ctutr

            UpeDetails(upeSafeId, Some(crn), Some(utr), name, LocalDate.now(), domesticOnly, nominateFm)

          case EntityType.LimitedLiabilityPartnership =>
            val partnershipEntityRegistrationData =
              upeGrsResponse.partnershipEntityRegistrationData.getOrElse(throw new Exception("Malformed LLP data"))
            val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
            val crn            = companyProfile.companyNumber
            val name           = companyProfile.companyName
            val utr            = partnershipEntityRegistrationData.sautr

            UpeDetails(upeSafeId, Some(crn), utr, name, LocalDate.now(), domesticOnly, nominateFm)

          case _ => throw new Exception("Invalid Org Type")
        }
      case false =>
        UpeDetails(upeSafeId, None, None, upeNameRegistration, LocalDate.now(), domesticOnly, nominateFm)
    }
  }

  private def getFilingMemberDetails(
    filingMemberSafeId: Option[String],
    fmRegisteredInUK:   Boolean,
    nominateFm:         Boolean,
    fmEntityTypeId:     EntityType,
    fmGrsResponseId:    GrsResponse,
    fmNameRegistration: String
  ): Option[FilingMemberDetails] =
    filingMemberSafeId match {
      case Some(fmSafeId) =>
        nominateFm match {
          case true =>
            fmRegisteredInUK match {
              case true =>
                fmEntityTypeId match {
                  case EntityType.UKLimitedCompany =>
                    val incorporatedEntityRegistrationData =
                      fmGrsResponseId.incorporatedEntityRegistrationData.getOrElse(
                        throw new Exception("Malformed IncorporatedEntityRegistrationData in Filing Member")
                      )
                    val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
                    val name = incorporatedEntityRegistrationData.companyProfile.companyName
                    val utr  = incorporatedEntityRegistrationData.ctutr

                    Some(FilingMemberDetails(fmSafeId, Some(crn), Some(utr), name))
                  case EntityType.LimitedLiabilityPartnership =>
                    val partnershipEntityRegistrationData =
                      fmGrsResponseId.partnershipEntityRegistrationData.getOrElse(
                        throw new Exception("Malformed partnershipEntityRegistrationData data for Filing Member")
                      )
                    val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
                    val crn            = companyProfile.companyNumber
                    val name           = companyProfile.companyName
                    val utr            = partnershipEntityRegistrationData.sautr
                    Some(FilingMemberDetails(fmSafeId, Some(crn), utr, name))

                  case _ => throw new Exception("Filing Member: Invalid Org Type")
                }
              case false =>
                Some(FilingMemberDetails(fmSafeId, None, None, fmNameRegistration))
              case _ => throw new Exception("Filing Member: Invalid Uk or other resident")
            }

          case false => None
        }
      case _ => None
    }

  private def getUpeAddressDetails(subAddressId: NonUKAddress): UpeCorrespAddressDetails =
    UpeCorrespAddressDetails(
      addressLine1 = subAddressId.addressLine1,
      addressLine2 = subAddressId.addressLine2,
      addressLine3 = Some(subAddressId.addressLine3),
      addressLine4 = subAddressId.addressLine4,
      postCode = subAddressId.postalCode,
      countryCode = subAddressId.countryCode
    )

  private def getAccountingPeriod(accountingPeriod: AccountingPeriod): AccountingPeriod =
    AccountingPeriod(accountingPeriod.startDate, accountingPeriod.endDate)

  private def getPrimaryContactDetails(primaryContactName: String, primaryPhone: String, primaryEmail: String): ContactDetailsType =
    ContactDetailsType(
      name = primaryContactName,
      telephone = Some(primaryPhone),
      emailAddress = primaryEmail
    )

  private def getSecondaryContactDetails(secondaryContactName: String, secondaryEmail: String, secondaryPhone: String): ContactDetailsType =
    ContactDetailsType(
      name = secondaryContactName,
      telephone = Some(secondaryPhone),
      emailAddress = secondaryEmail
    )

}
