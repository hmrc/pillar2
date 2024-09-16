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

import org.apache.pekko.Done
import cats.syntax.eq._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.SubscriptionConnector
import uk.gov.hmrc.pillar2.models.audit.AuditResponseReceived
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.registration.GrsResponse
import uk.gov.hmrc.pillar2.models.subscription.MneOrDomestic
import uk.gov.hmrc.pillar2.models.{AccountingPeriod, JsResultError, NonUKAddress, UnexpectedResponse, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.ReadSubscriptionCacheRepository
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.pillar2.models.queries.Gettable

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (
  repository:            ReadSubscriptionCacheRepository,
  subscriptionConnector: SubscriptionConnector,
  auditService:          AuditService
)(implicit ec:           ExecutionContext)
    extends Logging {

  def sendCreateSubscription(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                 HeaderCarrier
  ): Future[HttpResponse] = {
    userAnswers.get(NominateFilingMemberId.gettable) match {
      case Some(true) =>
        {
          (userAnswers.get(upeRegisteredInUKId.gettable), userAnswers.get(fmRegisteredInUKId.gettable)) match {
            case (Some(true), Some(true)) =>
              for {
                upeOrgType            <- userAnswers.get(upeEntityTypeId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                upeGrsResponse        <- userAnswers.get(upeGRSResponseId.gettable)
                fmEntityTypeId        <- userAnswers.get(fmEntityTypeId.gettable)
                fmGrsResponseId       <- userAnswers.get(fmGRSResponseId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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

            case (Some(false), Some(false)) =>
              for {
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                fmNameRegistration    <- userAnswers.get(fmNameRegistrationId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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

            case (Some(true), Some(false)) =>
              for {
                upeOrgType            <- userAnswers.get(upeEntityTypeId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                upeGrsResponse        <- userAnswers.get(upeGRSResponseId.gettable)
                fmNameRegistration    <- userAnswers.get(fmNameRegistrationId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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

            case (Some(false), Some(true)) =>
              for {
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                fmEntityTypeId        <- userAnswers.get(fmEntityTypeId.gettable)
                fmGrsResponseId       <- userAnswers.get(fmGRSResponseId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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
          }
        }.getOrElse {
          subscriptionError
        }

      case Some(false) =>
        {
          userAnswers.get(upeRegisteredInUKId.gettable) match {
            case Some(true) =>
              for {
                upeOrgType            <- userAnswers.get(upeEntityTypeId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                upeGrsResponse        <- userAnswers.get(upeGRSResponseId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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

            case Some(false) =>
              for {
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId.gettable)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId.gettable)
                nominateFm            <- userAnswers.get(NominateFilingMemberId.gettable)
                subAddressId          <- userAnswers.get(subRegisteredAddressId.gettable)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId.gettable)
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
          }
        }.getOrElse {
          subscriptionError
        }
    }
  }

  private def sendSubmissionRequest(subscriptionRequest: RequestDetail)(implicit
    hc:                                                  HeaderCarrier
  ): Future[HttpResponse] = {
    val response = subscriptionConnector
      .sendCreateSubscriptionInformation(subscriptionRequest)(hc, ec)
    response.map { res =>
      val resReceived = AuditResponseReceived(res.status, res.json)
      auditService.auditCreateSubscription(subscriptionRequest, resReceived)
    }
    response
  }

  private val subscriptionError = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription"))

  private def getWithIdUpeDetails(
    upeSafeId:        String,
    upeOrgType:       EntityType,
    subMneOrDomestic: MneOrDomestic,
    nominateFm:       Boolean,
    upeGrsResponse:   GrsResponse
  ): UpeDetails = {
    val domesticOnly = subMneOrDomestic === MneOrDomestic.uk
    upeOrgType match {
      case EntityType.UKLimitedCompany =>
        logger.info("UK Limited Company selected as Entity")
        upeGrsResponse.incorporatedEntityRegistrationData.fold {
          logger.error("Malformed Incorporation Registration Data")
          UpeDetails(Some(upeSafeId), None, None, "", LocalDate.now(), domesticOnly, nominateFm)
        } { incorporatedEntityRegistrationData =>
          val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
          val name = incorporatedEntityRegistrationData.companyProfile.companyName
          val utr  = incorporatedEntityRegistrationData.ctutr
          UpeDetails(Some(upeSafeId), Some(crn), Some(utr), name, LocalDate.now(), domesticOnly, nominateFm)
        }

      case EntityType.LimitedLiabilityPartnership =>
        logger.info("Limited Liability Partnership selected as Entity")
        upeGrsResponse.partnershipEntityRegistrationData.fold {
          logger.error("Malformed LLP data")
          UpeDetails(Some(upeSafeId), None, None, "", LocalDate.now(), domesticOnly, nominateFm)
        } { partnershipEntityRegistrationData =>
          partnershipEntityRegistrationData.companyProfile.fold {
            logger.error("Malformed company Profile")
            UpeDetails(Some(upeSafeId), None, None, "", LocalDate.now(), domesticOnly, nominateFm)
          } { companyProfile =>
            val crn  = companyProfile.companyNumber
            val name = companyProfile.companyName
            val utr  = partnershipEntityRegistrationData.sautr
            UpeDetails(Some(upeSafeId), Some(crn), utr, name, LocalDate.now(), domesticOnly, nominateFm)
          }
        }

      case _ =>
        logger.error("Invalid Org Type")
        UpeDetails(Some(upeSafeId), None, None, "", LocalDate.now(), domesticOnly, nominateFm)
    }
  }

  private def getWithoutIdUpeDetails(
    upeSafeId:           String,
    subMneOrDomestic:    MneOrDomestic,
    nominateFm:          Boolean,
    upeNameRegistration: String
  ): UpeDetails = {
    val domesticOnly = subMneOrDomestic === MneOrDomestic.uk
    UpeDetails(Some(upeSafeId), None, None, upeNameRegistration, LocalDate.now(), domesticOnly, nominateFm)
  }

  private def getWithIdFilingMemberDetails(
    filingMemberSafeId: Option[String],
    nominateFm:         Boolean,
    fmEntityTypeId:     EntityType,
    fmGrsResponseId:    GrsResponse
  ): Option[FilingMemberDetails] =
    filingMemberSafeId.flatMap { fmSafeId =>
      if (nominateFm) {
        fmEntityTypeId match {
          case EntityType.UKLimitedCompany =>
            fmGrsResponseId.incorporatedEntityRegistrationData.map { data =>
              FilingMemberDetails(fmSafeId, Some(data.companyProfile.companyNumber), Some(data.ctutr), data.companyProfile.companyName)
            }
          case EntityType.LimitedLiabilityPartnership =>
            fmGrsResponseId.partnershipEntityRegistrationData.flatMap { data =>
              data.companyProfile.map { profile =>
                FilingMemberDetails(fmSafeId, Some(profile.companyNumber), data.sautr, profile.companyName)
              }
            }
          case _ =>
            logger.error("Filing Member: Invalid Org Type")
            None
        }
      } else None
    }

  def getPrimaryContactInformation(userAnswers: UserAnswers): Option[ContactDetailsType] =
    for {
      primaryEmail       <- userAnswers.get(subPrimaryEmailId.gettable)
      primaryContactName <- userAnswers.get(subPrimaryContactNameId.gettable)
    } yield ContactDetailsType(name = primaryContactName, telephone = userAnswers.get(subPrimaryCapturePhoneId.gettable), emailAddress = primaryEmail)

  def getSecondaryContactInformation(userAnswers: UserAnswers): Option[ContactDetailsType] = {
    val doYouHaveSecondContact      = userAnswers.get(subAddSecondaryContactId.gettable)
    val doYouHaveSecondContactPhone = userAnswers.get(subSecondaryPhonePreferenceId.gettable)

    (doYouHaveSecondContact, doYouHaveSecondContactPhone) match {
      case (Some(false), _) => None
      case (Some(true), Some(_)) =>
        for {
          secondaryContactName <- userAnswers.get(subSecondaryContactNameId.gettable)
          secondaryEmail       <- userAnswers.get(subSecondaryEmailId.gettable)
        } yield ContactDetailsType(
          name = secondaryContactName,
          telephone = userAnswers.get(subSecondaryCapturePhoneId.gettable),
          emailAddress = secondaryEmail
        )
      case (Some(true), None) =>
        logger.error("subSecondaryPhonePreference Page not answered")
        None
      case (None, _) =>
        logger.error("doYouHaveSecondContact Page not answered")
        None
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
      dataToStore = createCachedObject(subscriptionResponse.success)
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

  private def createCachedObject(sub: SubscriptionSuccess): ReadSubscriptionCachedData = {

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
      .getOrElse(false, false)

    val secDetails: (Option[String], Option[String], Option[String]) = sub.secondaryContactDetails
      .map { sec =>
        (Some(sec.name), sec.telephone, Some(sec.emailAddress))
      }
      .getOrElse(None, None, None)

    ReadSubscriptionCachedData(
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
      subAccountingPeriod = accountingPeriod
    )
  }

  def sendAmendedData(id: String, amendData: AmendSubscriptionSuccess)(implicit hc: HeaderCarrier): Future[Done] =
    subscriptionConnector.amendSubscriptionInformation(amendData).flatMap { response =>
      if (response.status === 200) {
        auditService.auditAmendSubscription(requestData = amendData, responseData = AuditResponseReceived(response.status, response.json))
        response.json.validate[AmendResponse] match {
          case JsSuccess(result, _) =>
            logger.info(
              s"Successful response received for amend subscription for form ${result.success.formBundleNumber} at ${result.success.processingDate}"
            )
            repository.remove(id)
            Future.successful(Done)
          case _ => Future.failed(JsResultError)
        }
      } else {
        logger.info(
          s"Unsuccessful response received for amend subscription with ${response.status} status and body: ${response.body} "
        )
        auditService.auditAmendSubscription(requestData = amendData, responseData = AuditResponseReceived(response.status, response.json))
        Future.failed(UnexpectedResponse)
      }
    }
}
