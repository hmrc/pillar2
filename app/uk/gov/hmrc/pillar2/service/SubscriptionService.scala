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
import play.api.http.Status._
import play.api.libs.json.Writes._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.SubscriptionConnector
import uk.gov.hmrc.pillar2.models.audit.AuditResponseReceived
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.registration.GrsResponse
import uk.gov.hmrc.pillar2.models.subscription.{ExtraSubscription, MneOrDomestic}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, AccountingPeriodAmend, NonUKAddress, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class SubscriptionService @Inject() (
  repository:             RegistrationCacheRepository,
  subscriptionConnectors: SubscriptionConnector,
  countryOptions:         CountryOptions,
  auditService:           AuditService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def sendCreateSubscription(upeSafeId: String, fmSafeId: Option[String], userAnswers: UserAnswers)(implicit
    hc:                                 HeaderCarrier
  ): Future[HttpResponse] = {
    userAnswers.get(NominateFilingMemberId) match {
      case Some(true) =>
        {
          (userAnswers.get(upeRegisteredInUKId), userAnswers.get(fmRegisteredInUKId)) match {

            case (Some(true), Some(true)) =>
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
                logger.info("Calling sendSubmissionRequest with both upeRegisteredInUKId and fmRegisteredInUKId")
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
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
                nominateFm            <- userAnswers.get(NominateFilingMemberId)
                fmNameRegistration    <- userAnswers.get(fmNameRegistrationId)
                subAddressId          <- userAnswers.get(subRegisteredAddressId)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
                primaryContactDetails <- getPrimaryContactInformation(userAnswers)

              } yield {
                logger.info("Calling sendSubmissionRequest without upeRegisteredInUKId and fmRegisteredInUKId")
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
                upeOrgType            <- userAnswers.get(upeEntityTypeId)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
                nominateFm            <- userAnswers.get(NominateFilingMemberId)
                upeGrsResponse        <- userAnswers.get(upeGRSResponseId)
                fmNameRegistration    <- userAnswers.get(fmNameRegistrationId)
                subAddressId          <- userAnswers.get(subRegisteredAddressId)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
                primaryContactDetails <- getPrimaryContactInformation(userAnswers)

              } yield {
                logger.info("Calling sendSubmissionRequest with upeRegisteredInUKId")
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
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
                nominateFm            <- userAnswers.get(NominateFilingMemberId)
                fmEntityTypeId        <- userAnswers.get(fmEntityTypeId)
                fmGrsResponseId       <- userAnswers.get(fmGRSResponseId)
                subAddressId          <- userAnswers.get(subRegisteredAddressId)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
                primaryContactDetails <- getPrimaryContactInformation(userAnswers)

              } yield {
                logger.info("Calling sendSubmissionRequest with fmRegisteredInUKId")
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
          logger.warn("Subscription Information Missing")
          subscriptionError
        }

      case Some(false) =>
        {
          userAnswers.get(upeRegisteredInUKId) match {
            case Some(true) =>
              for {
                upeOrgType            <- userAnswers.get(upeEntityTypeId)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
                nominateFm            <- userAnswers.get(NominateFilingMemberId)
                upeGrsResponse        <- userAnswers.get(upeGRSResponseId)
                subAddressId          <- userAnswers.get(subRegisteredAddressId)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
                primaryContactDetails <- getPrimaryContactInformation(userAnswers)

              } yield {
                logger.info("Calling sendSubmissionRequest with upeRegisteredInUKId")
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
                upeNameRegistration   <- userAnswers.get(upeNameRegistrationId)
                subMneOrDomestic      <- userAnswers.get(subMneOrDomesticId)
                nominateFm            <- userAnswers.get(NominateFilingMemberId)
                subAddressId          <- userAnswers.get(subRegisteredAddressId)
                accountingPeriod      <- userAnswers.get(subAccountingPeriodId)
                primaryContactDetails <- getPrimaryContactInformation(userAnswers)

              } yield {
                logger.info("Calling sendSubmissionRequest without upeRegisteredInUKId")
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
          logger.warn("Subscription Information Missing")
          subscriptionError
        }
    }
  }

  private def sendSubmissionRequest(subscriptionRequest: RequestDetail)(implicit
    hc:                                                  HeaderCarrier,
    ec:                                                  ExecutionContext
  ): Future[HttpResponse] = {
    val response = subscriptionConnectors
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
                    throw new Exception("Malformed IncorporatedEntityRegistrationData in Filing Member")
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
            logger.info("Calling FilingMemberDetails with fmSafeId and fmNameRegistration")
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

  def processSuccessfulResponse(
    id:           String,
    plrReference: String,
    httpResponse: HttpResponse
  )(implicit
    ec:     ExecutionContext,
    hc:     HeaderCarrier,
    reads:  Reads[SubscriptionResponse],
    writes: Writes[UserAnswers]
  ): Future[JsValue] = {
    logger.info(s"SubscriptionService - ReadSubscription coming from Etmp - ${Json.prettyPrint(httpResponse.json)}")
    httpResponse.json.validate[SubscriptionResponse] match {
      case JsSuccess(subscriptionResponse, _) =>
        auditService.auditReadSubscriptionSuccess(plrReference, subscriptionResponse)
        extractSubscriptionData(id, plrReference, subscriptionResponse.success)
          .flatMap {
            case jsValue: JsObject =>
              jsValue.validate[UserAnswers] match {
                case JsSuccess(userAnswers, _) =>
                  repository.upsert(id, userAnswers.data).map { _ =>
                    logger.info(s"Upserted user answers for id: $id")
                    userAnswers.data
                  }
                case JsError(errors) =>
                  val errorDetails = errors
                    .map { case (path, validationErrors) =>
                      s"$path: ${validationErrors.mkString(", ")}"
                    }
                    .mkString("; ")
                  logger.error(s"Failed to convert json to UserAnswers: $errorDetails")
                  Future.failed(new Exception("Invalid user answers data"))
              }
            case _ =>
              Future.failed(new Exception("Invalid data type received from extractSubscriptionData"))
          }
          .recoverWith { case ex =>
            logger.error(s"Error processing successful response for id: $id", ex)
            Future.successful(Json.obj("error" -> ex.getMessage))
          }

      case JsError(errors) =>
        val errorDetails = errors
          .map { case (path, validationErrors) =>
            s"$path: ${validationErrors.mkString(", ")}"
          }
          .mkString("; ")
        logger.error(s"Failed to validate SubscriptionResponse: $errorDetails")
        Future.successful(Json.obj("error" -> "Invalid subscription response format"))
    }
  }

  def retrieveSubscriptionInformation(id: String, plrReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] =
    subscriptionConnectors
      .getSubscriptionInformation(plrReference)
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK => processSuccessfulResponse(id, plrReference, httpResponse)
          case _  => processErrorResponse(plrReference, httpResponse)
        }
      }
      .recover { case e: Exception =>
        logger.error("An error occurred while retrieving subscription information", e)
        Json.obj("error" -> e.getMessage)
      }

  private def processErrorResponse(plrReference: String, httpResponse: HttpResponse)(implicit
    hc:                                          HeaderCarrier,
    ec:                                          ExecutionContext
  ): Future[JsValue] = {
    val status = httpResponse.status
    //TODO failure needs to be audited as well. we dont have approval yet.
    val errorMessage = status match {
      case NOT_FOUND | BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
        s"Error response from service with status: $status and body: ${httpResponse.json}"
      case _ =>
        s"Unexpected response status from service: $status with body: ${httpResponse.json}"
    }
    logger.error(errorMessage)
    Future.successful(Json.obj("error" -> errorMessage))
  }

  def getNonEmptyOrNA(value: String): String =
    if (value.nonEmpty) value else "N/A"

  private def extractSubscriptionData(id: String, plrReference: String, sub: SubscriptionSuccess): Future[JsValue] = {

    val dashboardInfo = DashboardInfo(
      organisationName = sub.upeDetails.organisationName,
      registrationDate = sub.upeDetails.registrationDate
    )

    val nonUKAddress = NonUKAddress(
      addressLine1 = sub.upeCorrespAddressDetails.addressLine1,
      addressLine2 = sub.upeCorrespAddressDetails.addressLine2.filter(_.nonEmpty).orElse(Some("N/A")),
      addressLine3 = sub.upeCorrespAddressDetails.addressLine3 match {
        case Some(str) if str.nonEmpty => str
        case _                         => "N/A"
      },
      addressLine4 = sub.upeCorrespAddressDetails.addressLine4.filter(_.nonEmpty).orElse(Some("N/A")),
      postalCode = sub.upeCorrespAddressDetails.postCode.filter(_.nonEmpty).orElse(Some("N/A")),
      countryCode = sub.upeCorrespAddressDetails.countryCode
    )

    val crn    = sub.upeDetails.customerIdentification1
    val utr    = sub.upeDetails.customerIdentification2
    val safeId = sub.upeDetails.safeId
    //TODO - This needs refactoring
    val extraSubscription = ExtraSubscription(
      formBundleNumber = Some(getNonEmptyOrNA(sub.formBundleNumber)),
      crn = crn.map(getNonEmptyOrNA),
      utr = utr.map(getNonEmptyOrNA),
      safeId = safeId.map(getNonEmptyOrNA)
    )

    val filingMemberDetails = sub.filingMemberDetails.map { fMember =>
      FilingMemberDetails(
        safeId = fMember.safeId,
        customerIdentification1 = fMember.customerIdentification1,
        customerIdentification2 = fMember.customerIdentification2,
        organisationName = fMember.organisationName
      )
    }

    val accountingPeriod = AccountingPeriod(
      startDate = sub.accountingPeriod.startDate,
      endDate = sub.accountingPeriod.endDate,
      dueDate = sub.accountingPeriod.dueDate
    )

    val accountStatus = sub.accountStatus.map { acStatus =>
      AccountStatus(
        inactive = acStatus.inactive
      )
    }

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

    val subscriptionLocalData = SubscriptionLocalData(
      plrReference = plrReference,
      subMneOrDomestic = if (sub.upeDetails.domesticOnly) MneOrDomestic.Uk else MneOrDomestic.UkAndOther,
      upeNameRegistration = sub.upeDetails.organisationName,
      subPrimaryContactName = sub.primaryContactDetails.name,
      subPrimaryEmail = sub.primaryContactDetails.emailAddress,
      subPrimaryCapturePhone = sub.primaryContactDetails.telephone,
      subPrimaryPhonePreference = primaryHasTelephone,
      subSecondaryContactName = secDetails._1,
      subAddSecondaryContact = secContactTel._2,
      subSecondaryEmail = secDetails._3,
      subSecondaryCapturePhone = secDetails._2,
      subSecondaryPhonePreference = secContactTel._1,
      subRegisteredAddress = nonUKAddress,
      subFilingMemberDetails = filingMemberDetails,
      subAccountingPeriod = accountingPeriod,
      subAccountStatus = accountStatus,
      NominateFilingMember = sub.upeDetails.filingMember,
      subExtraSubscription = extraSubscription,
      subRegistrationDate = sub.upeDetails.registrationDate,
      fmDashboard = dashboardInfo
    )
    // TODO - this need refactoring. Best to save at backend only
    val userAnswers = UserAnswers(id, Json.toJsObject(subscriptionLocalData))
    Future.successful(Json.toJson(userAnswers))

  }

  private def createAmendSubscriptionParameters(userAnswers: UserAnswers): AmendSubscriptionSuccess = {
    logger.info(s"Starting extractAndProcess with UserAnswers: $userAnswers")
    (for {
      subAddress        <- userAnswers.get(subRegisteredAddressId)
      mneOrDom          <- userAnswers.get(subMneOrDomesticId)
      companyName       <- userAnswers.get(upeNameRegistrationId)
      pContactName      <- userAnswers.get(subPrimaryContactNameId)
      pContactEmail     <- userAnswers.get(subPrimaryEmailId)
      sContactNominated <- userAnswers.get(subAddSecondaryContactId)
      accountingPeriod  <- userAnswers.get(subAccountingPeriodId)
      nominatedFm       <- userAnswers.get(NominateFilingMemberId)
      registrationDate  <- userAnswers.get(subRegistrationDateId)
      plrReference      <- userAnswers.get(plrReferenceId)
    } yield {
      val upeDetail = UpeDetailsAmend(
        plrReference = plrReference,
        customerIdentification1 = userAnswers.get(upeRegInformationId).map(_.crn),
        customerIdentification2 = userAnswers.get(upeRegInformationId).map(_.utr),
        organisationName = companyName,
        registrationDate = registrationDate,
        domesticOnly = if (mneOrDom == MneOrDomestic.Uk) true else false,
        filingMember = nominatedFm
      )
      val primaryContact =
        ContactDetailsType(name = pContactName, telephone = userAnswers.get(subPrimaryCapturePhoneId), emailAddress = pContactEmail)
      val secondaryContact = if (sContactNominated) {
        for {
          name         <- userAnswers.get(subSecondaryContactNameId)
          emailAddress <- userAnswers.get(subSecondaryEmailId)
        } yield ContactDetailsType(name = name, telephone = userAnswers.get(subSecondaryCapturePhoneId), emailAddress = emailAddress)
      } else {
        None
      }

      val filingMember = if (nominatedFm) {
        userAnswers
          .get(subFilingMemberDetailsId)
          .map(det =>
            FilingMemberAmendDetails(
              safeId = det.safeId,
              customerIdentification1 = det.customerIdentification1,
              customerIdentification2 = det.customerIdentification2,
              organisationName = det.organisationName
            )
          )
      } else {
        None
      }
      AmendSubscriptionSuccess(
        upeDetails = upeDetail,
        accountingPeriod = AccountingPeriodAmend(accountingPeriod.startDate, accountingPeriod.endDate),
        upeCorrespAddressDetails = UpeCorrespAddressDetails(
          subAddress.addressLine1,
          subAddress.addressLine2,
          Some(subAddress.addressLine3),
          subAddress.addressLine4,
          subAddress.postalCode,
          subAddress.countryCode
        ),
        primaryContactDetails = primaryContact,
        secondaryContactDetails = secondaryContact,
        filingMemberDetails = filingMember
      )
    }).getOrElse(throw new Exception("Expected data missing from user answers"))
  }
  def extractAndProcess(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    if (userAnswers == null) {
      logger.error("UserAnswers is null")
      Future.failed(new IllegalArgumentException("UserAnswers cannot be null"))
    } else {
      val amendSub = createAmendSubscriptionParameters(userAnswers)
      logger.info(s"SubscriptionService - AmendSubscription going to Etmp - ${Json.prettyPrint(Json.toJson(amendSub))}")

      subscriptionConnectors.amendSubscriptionInformation(amendSub).flatMap { response =>
        if (response.status == 200) {
          auditService.auditAmendSubscription(requestData = amendSub, responseData = AuditResponseReceived(response.status, response.json))
          response.json.validate[AmendResponse] match {
            case JsSuccess(result, _) =>
              logger
                .info(
                  s"Successful response received for amend subscription for form ${result.success.formBundleNumber} at ${result.success.processingDate}"
                )
              Future.successful(response)
            case _ => throw new Exception("Could not parse response received from ETMP in success response")
          }
        } else {
          auditService.auditAmendSubscription(requestData = amendSub, responseData = AuditResponseReceived(response.status, response.json))
          response.json.validate[AmendSubscriptionFailureResponse] match {
            case JsSuccess(failure, _) =>
              logger.info(s"Call failed to ETMP with the code ${failure.failures(0).code} due to ${failure.failures(0).reason}")
              Future.successful(response)
            case _ => throw new Exception(s"Could not parse error response received from ETMP in failure response")

          }
        }

      }
    }

}
