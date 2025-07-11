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
import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.SubscriptionConnector
import uk.gov.hmrc.pillar2.models._
import uk.gov.hmrc.pillar2.models.audit.AuditResponseReceived
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.registration.GrsResponse
import uk.gov.hmrc.pillar2.models.subscription.MneOrDomestic
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class SubscriptionService @Inject() (
  repository:            ReadSubscriptionCacheRepository,
  subscriptionConnector: SubscriptionConnector,
  auditService:          AuditService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendCreateSubscription(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                 HeaderCarrier
  ): Future[HttpResponse] =
    userAnswers.get(NominateFilingMemberId) match {
      case Some(true) =>
        (userAnswers.get(upeRegisteredInUKId), userAnswers.get(fmRegisteredInUKId)) match {

          case (Some(true), Some(true)) =>
            bothRegisteredInUK(upeSafeId, fmSafeId, userAnswers).getOrElse(subscriptionError)
          case (Some(false), Some(false)) =>
            bothOutsideUK(upeSafeId, fmSafeId, userAnswers).getOrElse(subscriptionError)
          case (Some(true), Some(false)) =>
            upeInUKFMOutsideUK(upeSafeId, fmSafeId, userAnswers).getOrElse(subscriptionError)
          case (Some(false), Some(true)) =>
            upeOutsideUKFMInUK(upeSafeId, fmSafeId, userAnswers).getOrElse(subscriptionError)
          case _ => subscriptionError
        }

      case Some(false) =>
        userAnswers.get(upeRegisteredInUKId) match {
          case Some(true) =>
            upeRegisteredInUK(upeSafeId, userAnswers).getOrElse {
              subscriptionError
            }
          case Some(false) =>
            upeRegisteredOutsideUK(upeSafeId, userAnswers).getOrElse {
              subscriptionError
            }
          case None => subscriptionError
        }
      case None => subscriptionError
    }

  private def upeRegisteredOutsideUK(upeSafeId: String, userAnswers: UserAnswers)(implicit
    hc:                                         HeaderCarrier
  ) =
    for {
      upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, !nominateFm, upeNameRegistration),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        None
      )
      sendSubmissionRequest(subscriptionRequest)
    }

  private def upeRegisteredInUK(upeSafeId: String, userAnswers: UserAnswers)(implicit
    hc:                                    HeaderCarrier
  ) =
    for {
      upeOrgType            <- userAnswers.get(upeEntityTypeId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      upeGrsResponse        <- userAnswers.get(upeGRSResponseId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, !nominateFm, upeGrsResponse),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        None
      )

      sendSubmissionRequest(subscriptionRequest)
    }

  private def upeOutsideUKFMInUK(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                     HeaderCarrier
  ) =
    for {
      upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      fmEntityTypeId        <- userAnswers.get(fmEntityTypeId)
      fmGrsResponseId       <- userAnswers.get(fmGRSResponseId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, !nominateFm, upeNameRegistration),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        getWithIdFilingMemberDetails(fmSafeId, nominateFm, fmEntityTypeId, fmGrsResponseId)
      )

      sendSubmissionRequest(subscriptionRequest)
    }

  private def upeInUKFMOutsideUK(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                     HeaderCarrier
  ) =
    for {
      upeOrgType            <- userAnswers.get(upeEntityTypeId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      upeGrsResponse        <- userAnswers.get(upeGRSResponseId)
      fmNameRegistration    <- userAnswers.get(fmNameRegistrationId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, !nominateFm, upeGrsResponse),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        getWithoutIdFilingMemberDetails(fmSafeId, nominateFm, fmNameRegistration)
      )

      sendSubmissionRequest(subscriptionRequest)
    }

  private def bothOutsideUK(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                HeaderCarrier
  ) =
    for {
      upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      fmNameRegistration    <- userAnswers.get(fmNameRegistrationId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, !nominateFm, upeNameRegistration),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        getWithoutIdFilingMemberDetails(fmSafeId, nominateFm, fmNameRegistration)
      )

      sendSubmissionRequest(subscriptionRequest)
    }

  private def bothRegisteredInUK(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                     HeaderCarrier
  ) =
    for {
      upeOrgType            <- userAnswers.get(upeEntityTypeId)
      subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
      nominateFm            <- userAnswers.get(NominateFilingMemberId)
      upeGrsResponse        <- userAnswers.get(upeGRSResponseId)
      fmEntityTypeId        <- userAnswers.get(fmEntityTypeId)
      fmGrsResponseId       <- userAnswers.get(fmGRSResponseId)
      subAddressId          <- userAnswers.get(subRegisteredAddressId)
      accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
      primaryContactDetails <- getPrimaryContactInformation(userAnswers)

    } yield {
      val subscriptionRequest = RequestDetail(
        getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, !nominateFm, upeGrsResponse),
        getAccountingPeriod(accountingPeriod),
        getUpeAddressDetails(subAddressId),
        primaryContactDetails,
        getSecondaryContactInformation(userAnswers),
        getWithIdFilingMemberDetails(fmSafeId, nominateFm, fmEntityTypeId, fmGrsResponseId)
      )

      sendSubmissionRequest(subscriptionRequest)
    }

  private def sendSubmissionRequest(subscriptionRequest: RequestDetail)(implicit
    hc:                                                  HeaderCarrier
  ): Future[HttpResponse] =
    subscriptionConnector
      .sendCreateSubscriptionInformation(subscriptionRequest)(hc, ec)
      .flatTap { res =>
        val resReceived = AuditResponseReceived(res.status, res.json)
        auditService.auditCreateSubscription(subscriptionRequest, resReceived)
      }

  private val subscriptionError = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription"))

  private def getWithIdUpeDetails(
    upeSafeId:        String,
    upeOrgType:       EntityType,
    subMneOrDomestic: MneOrDomestic,
    nominateFm:       Boolean,
    upeGrsResponse:   GrsResponse
  ): UpeDetails = {
    val domesticOnly = if (subMneOrDomestic == MneOrDomestic.uk) true else false
    upeOrgType match {
      case EntityType.UKLimitedCompany =>
        logger.info("UK Limited Company selected as Entity")
        val incorporatedEntityRegistrationData =
          upeGrsResponse.incorporatedEntityRegistrationData.getOrElse(throw new Exception("Malformed Incorporation Registration Data"))
        val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
        val name = incorporatedEntityRegistrationData.companyProfile.companyName
        val utr  = incorporatedEntityRegistrationData.ctutr

        UpeDetails(Some(upeSafeId), Some(crn), Some(utr), name, LocalDate.now(), domesticOnly, nominateFm)

      case EntityType.LimitedLiabilityPartnership =>
        logger.info("Limited Liability Partnership selected as Entity")
        val partnershipEntityRegistrationData =
          upeGrsResponse.partnershipEntityRegistrationData.getOrElse(throw new Exception("Malformed LLP data"))
        val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
        val crn            = companyProfile.companyNumber
        val name           = companyProfile.companyName
        val utr            = partnershipEntityRegistrationData.sautr

        UpeDetails(Some(upeSafeId), Some(crn), utr, name, LocalDate.now(), domesticOnly, nominateFm)

      case _ => throw new Exception("Invalid Org Type")
    }
  }

  private def getWithoutIdUpeDetails(
    upeSafeId:           String,
    subMneOrDomestic:    MneOrDomestic,
    nominateFm:          Boolean,
    upeNameRegistration: String
  ): UpeDetails = {
    val domesticOnly = if (subMneOrDomestic == MneOrDomestic.uk) true else false
    UpeDetails(Some(upeSafeId), None, None, upeNameRegistration, LocalDate.now(), domesticOnly, nominateFm)

  }

  private def getWithIdFilingMemberDetails(
    filingMemberSafeId: Option[String],
    nominateFm:         Boolean,
    fmEntityTypeId:     EntityType,
    fmGrsResponseId:    GrsResponse
  ): Option[FilingMemberDetails] =
    filingMemberSafeId match {
      case Some(fmSafeId) =>
        logger.info("filingMemberSafeId is matched")
        nominateFm match {
          case true =>
            logger.info("nominateFm value is True")
            fmEntityTypeId match {
              case EntityType.UKLimitedCompany =>
                logger.info("UK Limited Company selected as Entity")
                val incorporatedEntityRegistrationData =
                  fmGrsResponseId.incorporatedEntityRegistrationData.getOrElse(
                    throw new Exception(
                      "Malformed IncorporatedEntityRegistrationData in Filing Member"
                    )
                  )
                val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
                val name = incorporatedEntityRegistrationData.companyProfile.companyName
                val utr  = incorporatedEntityRegistrationData.ctutr

                Some(FilingMemberDetails(fmSafeId, Some(crn), Some(utr), name))
              case EntityType.LimitedLiabilityPartnership =>
                logger.info("Limited Liability Corporation selected as Entity")
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
            logger.info("nominateFm value is False")
            None
        }
      case _ => None
    }

  def getPrimaryContactInformation(userAnswers: UserAnswers): Option[ContactDetailsType] =
    for {
      primaryEmail       <- userAnswers.get(subPrimaryEmailId)
      primaryContactName <- userAnswers.get(subPrimaryContactNameId)
    } yield ContactDetailsType(name = primaryContactName, telephone = userAnswers.get(subPrimaryCapturePhoneId), emailAddress = primaryEmail)

  def getSecondaryContactInformation(userAnswers: UserAnswers): Option[ContactDetailsType] = {
    val doYouHaveSecondContact      = userAnswers.get(subAddSecondaryContactId)
    val doYouHaveSecondContactPhone = userAnswers.get(subSecondaryPhonePreferenceId)

    (doYouHaveSecondContact, doYouHaveSecondContactPhone) match {
      case (Some(false), _) => None
      case (Some(true), Some(_)) =>
        for {
          secondaryContactName <- userAnswers.get(subSecondaryContactNameId)
          secondaryEmail       <- userAnswers.get(subSecondaryEmailId)
        } yield ContactDetailsType(
          name = secondaryContactName,
          telephone = userAnswers.get(subSecondaryCapturePhoneId),
          emailAddress = secondaryEmail
        )

      case (Some(true), None) => throw new Exception("subSecondaryPhonePreference Page not answered")
      case (None, _)          => throw new Exception("doYouHaveSecondContact Page not answered")
    }
  }

  private def getWithoutIdFilingMemberDetails(
    filingMemberSafeId: Option[String],
    nominateFm:         Boolean,
    fmNameRegistration: String
  ): Option[FilingMemberDetails] =
    filingMemberSafeId match {
      case Some(fmSafeId) =>
        nominateFm match {
          case true =>
            Some(FilingMemberDetails(fmSafeId, None, None, fmNameRegistration))
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

  def storeSubscriptionResponse(id: String, plrReference: String)(implicit hc: HeaderCarrier): Future[SubscriptionResponse] =
    for {
      response <- subscriptionConnector.getSubscriptionInformation(plrReference)
      subscriptionResponse = response.json.as[SubscriptionResponse]
      _ <- auditService.auditReadSubscriptionSuccess(plrReference, subscriptionResponse)
      dataToStore = createCachedObject(subscriptionResponse.success, plrReference)
      _ <- repository.upsert(id, Json.toJson(dataToStore))
    } yield subscriptionResponse

  def readSubscriptionData(plrReference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    for {
      response <- subscriptionConnector.getSubscriptionInformation(plrReference)
      _        <- createAuditForReadSubscription(plrReference, response)
    } yield response

  private def createAuditForReadSubscription(plrReference: String, response: HttpResponse)(implicit hc: HeaderCarrier): Future[AuditResult] =
    response.status match {
      case 200 => auditService.auditReadSubscriptionSuccess(plrReference, response.json.as[SubscriptionResponse])
      case _   => auditService.auditReadSubscriptionFailure(plrReference, response.status, response.json)
    }

  private def createCachedObject(sub: SubscriptionSuccess, plrReference: String): ReadSubscriptionCachedData = {

    val nonUKAddress = NonUKAddress(
      addressLine1 = sub.upeCorrespAddressDetails.addressLine1,
      addressLine2 = sub.upeCorrespAddressDetails.addressLine2.filter(_.nonEmpty),
      addressLine3 = sub.upeCorrespAddressDetails.addressLine3.getOrElse(""),
      addressLine4 = sub.upeCorrespAddressDetails.addressLine4.filter(_.nonEmpty),
      postalCode = sub.upeCorrespAddressDetails.postCode.filter(_.nonEmpty),
      countryCode = sub.upeCorrespAddressDetails.countryCode
    )
    val accountingPeriod = AccountingPeriod(
      startDate = sub.accountingPeriod.startDate,
      endDate = sub.accountingPeriod.endDate,
      dueDate = sub.accountingPeriod.dueDate
    )
    val primaryHasTelephone: Boolean = sub.primaryContactDetails.telephone.isDefined

    val secContactTel: (Boolean, Boolean) = sub.secondaryContactDetails
      .map { sContact =>
        (sContact.telephone.isDefined, sContact.telephone.exists(_.nonEmpty) || sContact.emailAddress.nonEmpty || sContact.name.nonEmpty)
      }
      .getOrElse(false -> false)

    val secDetails: (Option[String], Option[String], Option[String]) = sub.secondaryContactDetails
      .map { sec =>
        (Some(sec.name), sec.telephone, Some(sec.emailAddress))
      }
      .getOrElse((None, None, None))

    val organisationName: Option[String] = Some(sub.upeDetails.organisationName)

    ReadSubscriptionCachedData(
      plrReference = Some(plrReference),
      subMneOrDomestic = if (sub.upeDetails.domesticOnly) MneOrDomestic.Uk else MneOrDomestic.UkAndOther,
      subPrimaryContactName = sub.primaryContactDetails.name,
      subPrimaryEmail = sub.primaryContactDetails.emailAddress,
      subPrimaryCapturePhone = sub.primaryContactDetails.telephone,
      subPrimaryPhonePreference = primaryHasTelephone,
      subSecondaryContactName = secDetails._1,
      subAddSecondaryContact = secContactTel._2,
      subSecondaryEmail = secDetails._3,
      subSecondaryCapturePhone = secDetails._2,
      subSecondaryPhonePreference = Some(secContactTel._1),
      subRegisteredAddress = nonUKAddress,
      subAccountingPeriod = accountingPeriod,
      accountStatus = sub.accountStatus,
      organisationName = organisationName
    )
  }

  def sendAmendedData(id: String, amendData: AmendSubscriptionSuccess)(implicit hc: HeaderCarrier): Future[Done] = {
    val etmpAmendSubscriptionSuccess = ETMPAmendSubscriptionSuccess(amendData)
    subscriptionConnector.amendSubscriptionInformation(etmpAmendSubscriptionSuccess).flatMap { response =>
      if (response.status == 200) {
        auditService.auditAmendSubscription(requestData = amendData, responseData = AuditResponseReceived(response.status, response.json)) >> {
          response.json.validate[AmendResponse] match {
            case JsSuccess(result, _) =>
              logger.info(
                s"Successful response received for amend subscription for form ${result.success.formBundleNumber} at ${result.success.processingDate}"
              )
              repository.remove(id) >>
                Future.successful(Done)
            case _ => Future.failed(JsResultError)
          }
        }
      } else {
        logger.info(
          s"Unsuccessful response received for amend subscription with ${response.status} status and body: ${response.body} "
        )
        auditService.auditAmendSubscription(requestData = amendData, responseData = AuditResponseReceived(response.status, response.json)) >>
          Future.failed(UnexpectedResponse)
      }
    }
  }

}
