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
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.{CreateSubscriptionRequest, RequestDetail, SubscriptionRequest}
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.registration.{GrsResponse, RegistrationInfo}
import uk.gov.hmrc.pillar2.models.subscription.MneOrDomestic
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, NonUKAddress, UKAddress, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions
import scala.concurrent.Future
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.pillar2.models._
import uk.gov.hmrc.pillar2.models.hods.subscription.common.SubscriptionSuccess
import uk.gov.hmrc.pillar2.models.UserAnswers.format

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, nominateFm, upeGrsResponse),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      getWithIdFilingMemberDetails(fmSafeId, nominateFm, fmEntityTypeId, fmGrsResponseId)
                    )
                  )
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, nominateFm, upeNameRegistration),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      getWithoutIdFilingMemberDetails(fmSafeId, nominateFm, fmNameRegistration)
                    )
                  )
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, nominateFm, upeGrsResponse),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      getWithoutIdFilingMemberDetails(fmSafeId, nominateFm, fmNameRegistration)
                    )
                  )
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, nominateFm, upeNameRegistration),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      getWithIdFilingMemberDetails(fmSafeId, nominateFm, fmEntityTypeId, fmGrsResponseId)
                    )
                  )
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithIdUpeDetails(upeSafeId, upeOrgType, subMneOrDomestic, nominateFm, upeGrsResponse),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      None
                    )
                  )
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
                val subscriptionRequest = CreateSubscriptionRequest(
                  createSubscriptionRequest = SubscriptionRequest(
                    requestBody = RequestDetail(
                      getWithoutIdUpeDetails(upeSafeId, subMneOrDomestic, nominateFm, upeNameRegistration),
                      getAccountingPeriod(accountingPeriod),
                      getUpeAddressDetails(subAddressId),
                      primaryContactDetails,
                      getSecondaryContactInformation(userAnswers),
                      None
                    )
                  )
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

  private def sendSubmissionRequest(subscriptionRequest: CreateSubscriptionRequest)(implicit
    hc:                                                  HeaderCarrier,
    ec:                                                  ExecutionContext
  ): Future[HttpResponse] =
    subscriptionConnectors
      .sendCreateSubscriptionInformation(subscriptionRequest)(hc, ec)

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
        val incorporatedEntityRegistrationData =
          upeGrsResponse.incorporatedEntityRegistrationData.getOrElse(throw new Exception("Malformed Incorporation Registration Data"))
        val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
        val name = incorporatedEntityRegistrationData.companyProfile.companyName
        val utr  = incorporatedEntityRegistrationData.ctutr

        UpeDetails(None, Some(upeSafeId), Some(crn), Some(utr), name, LocalDate.now(), domesticOnly, nominateFm)

      case EntityType.LimitedLiabilityPartnership =>
        val partnershipEntityRegistrationData =
          upeGrsResponse.partnershipEntityRegistrationData.getOrElse(throw new Exception("Malformed LLP data"))
        val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
        val crn            = companyProfile.companyNumber
        val name           = companyProfile.companyName
        val utr            = partnershipEntityRegistrationData.sautr

        UpeDetails(None, Some(upeSafeId), Some(crn), utr, name, LocalDate.now(), domesticOnly, nominateFm)

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
    UpeDetails(None, Some(upeSafeId), None, None, upeNameRegistration, LocalDate.now(), domesticOnly, nominateFm)

  }

  private def getWithIdFilingMemberDetails(
    filingMemberSafeId: Option[String],
    nominateFm:         Boolean,
    fmEntityTypeId:     EntityType,
    fmGrsResponseId:    GrsResponse
  ): Option[FilingMemberDetails] =
    filingMemberSafeId match {
      case Some(fmSafeId) =>
        nominateFm match {
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

                Some(FilingMemberDetails(None, fmSafeId, Some(crn), Some(utr), name))
              case EntityType.LimitedLiabilityPartnership =>
                val partnershipEntityRegistrationData =
                  fmGrsResponseId.partnershipEntityRegistrationData.getOrElse(
                    throw new Exception("Malformed partnershipEntityRegistrationData data for Filing Member")
                  )
                val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
                val crn            = companyProfile.companyNumber
                val name           = companyProfile.companyName
                val utr            = partnershipEntityRegistrationData.sautr
                Some(FilingMemberDetails(None, fmSafeId, Some(crn), utr, name))

              case _ => throw new Exception("Filing Member: Invalid Org Type")
            }

          case false => None
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
            Some(FilingMemberDetails(None, fmSafeId, None, None, fmNameRegistration))
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
    httpResponse: HttpResponse
  )(implicit
    ec:     ExecutionContext,
    reads:  Reads[SubscriptionResponse],
    writes: Writes[UserAnswers]
  ): Future[JsValue] =
    httpResponse.json.validate[SubscriptionResponse] match {
      case JsSuccess(subscriptionResponse, _) =>
        extractSubscriptionData(id, subscriptionResponse.success)
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

  def retrieveSubscriptionInformation(id: String, plrReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] =
    subscriptionConnectors
      .getSubscriptionInformation(plrReference)
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK => processSuccessfulResponse(id, httpResponse)
          case _  => processErrorResponse(httpResponse)
        }
      }
      .recover { case e: Exception =>
        logger.error("An error occurred while retrieving subscription information", e)
        Json.obj("error" -> e.getMessage)
      }

  private def processErrorResponse(httpResponse: HttpResponse): Future[JsValue] = {
    val status = httpResponse.status
    val errorMessage = status match {
      case NOT_FOUND | BAD_REQUEST | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
        s"Error response from service with status: $status and body: ${httpResponse.body}"
      case _ =>
        s"Unexpected response status from service: $status with body: ${httpResponse.body}"
    }
    logger.error(errorMessage)
    Future.successful(Json.obj("error" -> errorMessage))
  }

  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess, opType: Int = 0): Future[JsValue] = {
    val userAnswers = UserAnswers(id, Json.obj())

    val registrationInfo = RegistrationInfo(
      crn = sub.upeDetails.customerIdentification1.getOrElse(" "),
      utr = sub.upeDetails.customerIdentification2.getOrElse(" "),
      safeId = sub.upeDetails.safeId.getOrElse(""),
      registrationDate = Option(sub.upeDetails.registrationDate),
      filingMember = Option(sub.upeDetails.filingMember)
    )

    val ukAddress = UKAddress(
      addressLine1 = sub.upeCorrespAddressDetails.addressLine1,
      addressLine2 = sub.upeCorrespAddressDetails.addressLine2,
      addressLine3 = sub.upeCorrespAddressDetails.addressLine3.getOrElse(""),
      addressLine4 = sub.upeCorrespAddressDetails.addressLine4,
      postalCode = sub.upeCorrespAddressDetails.postCode.getOrElse(""),
      countryCode = sub.upeCorrespAddressDetails.countryCode
    )

    val filingMemberDetails = FilingMemberDetails(
      addNewFilingMember = None,
      safeId = sub.filingMemberDetails.safeId,
      customerIdentification1 = sub.filingMemberDetails.customerIdentification1,
      customerIdentification2 = sub.filingMemberDetails.customerIdentification2,
      organisationName = sub.filingMemberDetails.organisationName
    )

    val accountingPeriod = if (opType == 0) {
      AccountingPeriod(sub.accountingPeriod.startDate, sub.accountingPeriod.endDate, sub.accountingPeriod.duetDate)
    } else {
      AccountingPeriod(sub.accountingPeriod.startDate, sub.accountingPeriod.endDate)
    }

    val accountStatusOpt: Option[AccountStatus] = if (opType == 0) Some(sub.accountStatus) else None

    val result = for {
      u1 <- Future.fromTry(userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk))
      u2 <- Future.fromTry(u1.set(upeNameRegistrationId, sub.upeDetails.organisationName))
      u3 <- Future.fromTry(u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name))
      u4 <- Future.fromTry(u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress))
      u5 <- Future.fromTry(u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name))
      u6 <- Future.fromTry(u5.set(upeRegInformationId, registrationInfo))
      u7 <- Future.fromTry(u6.set(upeRegisteredAddressId, ukAddress))
      u8 <- Future.fromTry(u7.set(FmSafeId, sub.filingMemberDetails.safeId))
      u9 <- Future.fromTry(u8.set(subFilingMemberDetailsId, filingMemberDetails))
      u10 <- accountStatusOpt match {
               case Some(status) => Future.fromTry(u9.set(subAccountStatusId, status))
               case None         => Future.successful(u9)
             }
      telephoneStr = sub.secondaryContactDetails.telepphone.getOrElse("")
      finalUserAnswers <- Future.fromTry(u10.set(subSecondaryCapturePhoneId, telephoneStr))
    } yield finalUserAnswers

    result
      .map(Json.toJson(_))
      .recover { case exception =>
        logger.error("An error occurred while extracting subscription data", exception)
        Json.obj("error" -> exception.getMessage)
      }
  }

  //      val result = for {
//      u1 <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
//      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
//      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
//      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
//      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
//      u6 <- u5.set(upeRegInformationId, registrationInfo)
//      u7 <- u6.set(upeRegisteredAddressId, ukAddress)
//      u8 <- u7.set(FmSafeId, sub.filingMemberDetails.safeId)
//      u9 <- u8.set(subFilingMemberDetailsId, filingMemberDetails)
//        .flatMap { updatedUA =>
//          accountStatusOpt match {
//            case Some(status) => updatedUA.set(subAccountStatusId, status)
//            case None => Success(updatedUA)
//          }
//        }
//      telephoneStr = sub.secondaryContactDetails.telepphone.getOrElse("")
//      finalUserAnswers <- Future.fromTry(u10.set(subSecondaryCapturePhoneId, telephoneStr))
////      telephoneStr = sub.secondaryContactDetails.telepphone.getOrElse("")
////      finalUserAnswers <- userAnswers.set(subSecondaryCapturePhoneId, telephoneStr)
//    } yield finalUserAnswers
//
//    result match {
////      case Success(userAnswers) => Future.successful(Json.toJson(userAnswers))
//      case Success(userAnswers) => Future.successful(Json.toJson(userAnswers)(UserAnswers.format))
//      case Failure(exception) =>
//        logger.error("An error occurred while extracting subscription data", exception)
//        Future.successful(Json.obj("error" -> exception.getMessage))
//    }
//  }

  //  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess, opType: Int = 0): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    val registrationInfo = RegistrationInfo(
//      crn = sub.upeDetails.customerIdentification1.getOrElse(" "),
//      utr = sub.upeDetails.customerIdentification2.getOrElse(" "),
//      safeId = sub.upeDetails.safeId.getOrElse(""),
//      registrationDate = Option(sub.upeDetails.registrationDate),
//      filingMember = Option(sub.upeDetails.filingMember)
//    )
//
//    val ukAddress = UKAddress(
//      addressLine1 = sub.upeCorrespAddressDetails.addressLine1,
//      addressLine2 = sub.upeCorrespAddressDetails.addressLine2,
//      addressLine3 = sub.upeCorrespAddressDetails.addressLine3.getOrElse(""),
//      addressLine4 = sub.upeCorrespAddressDetails.addressLine4,
//      postalCode = sub.upeCorrespAddressDetails.postCode.getOrElse(""),
//      countryCode = sub.upeCorrespAddressDetails.countryCode
//    )
//
//    val filingMemberDetails = FilingMemberDetails(
//      addNewFilingMember = None,
//      safeId = sub.filingMemberDetails.safeId,
//      customerIdentification1 = sub.filingMemberDetails.customerIdentification1,
//      customerIdentification2 = sub.filingMemberDetails.customerIdentification2,
//      organisationName = sub.filingMemberDetails.organisationName
//    )
//
//    val accountStatusOpt: Option[AccountStatus] = if (opType == 0) {
//      Option(sub.accountStatus)
//    } else {
//      None
//    }
//
//    val accountingPeriod = opType match {
//      case 0 => AccountingPeriod(
//        startDate = sub.accountingPeriod.startDate,
//        endDate = sub.accountingPeriod.endDate,
//        duetDate = sub.accountingPeriod.duetDate
//      )
//      case 1 => AccountingPeriod(
//        startDate = sub.accountingPeriod.startDate,
//        endDate = sub.accountingPeriod.endDate
//      )
//      case _ => throw new IllegalArgumentException("Invalid operation type")
//    }
//
//    val result = for {
//      u1 <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
//      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
//      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
//      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
//      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
//      u6 <- u5.set(upeRegInformationId, registrationInfo)
//      u7 <- u6.set(upeRegisteredAddressId, ukAddress)
//      u8 <- u7.set(FmSafeId, sub.filingMemberDetails.safeId)
//      u9 <- u8.set(subFilingMemberDetailsId, filingMemberDetails)
//      u10 <- u9.set(subAccountingPeriodId, accountingPeriod)
//      userAnswers <- accountStatusOpt match {
//        case Some(status) => u10.set(subAccountStatusId, status)
//        case None => Future.successful(u10)
//      }
//      telephoneStr = sub.secondaryContactDetails.telepphone.getOrElse("")
//      finalUserAnswers <- userAnswers.set(subSecondaryCapturePhoneId, telephoneStr)
//    } yield finalUserAnswers
//
////    result match {
////      case Success(ua) => Future.successful(Json.toJson(ua)(writes)) // Use the implicit Writes instance
////      case Failure(exception) =>
////        logger.error("An error occurred while extracting subscription data", exception)
////        Future.successful(Json.obj("error" -> exception.getMessage))
////    }
//
//    result match {
//      case Success(ua) => Future.successful(Json.toJson(ua))
//      case Failure(exception) =>
//        logger.error("An error occurred while extracting subscription data", exception)
//        Future.successful(Json.obj("error" -> exception.getMessage))
//    }
//  }

//  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess, opType:Int=0): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    val registrationInfo = RegistrationInfo(
//      crn = sub.upeDetails.customerIdentification1.getOrElse(" "),
//      utr = sub.upeDetails.customerIdentification2.getOrElse(" "),
//      safeId = sub.upeDetails.safeId.getOrElse(""),
//      registrationDate = Option(sub.upeDetails.registrationDate),
//      filingMember = Option(sub.upeDetails.filingMember)
//    )
//
//    val ukAddress = UKAddress(
//      addressLine1 = sub.upeCorrespAddressDetails.addressLine1,
//      addressLine2 = sub.upeCorrespAddressDetails.addressLine2,
//      addressLine3 = sub.upeCorrespAddressDetails.addressLine3.getOrElse(""),
//      addressLine4 = sub.upeCorrespAddressDetails.addressLine4,
//      postalCode = sub.upeCorrespAddressDetails.postCode.getOrElse(""),
//      countryCode = sub.upeCorrespAddressDetails.countryCode
//    )
//
//    val filingMemberDetails = FilingMemberDetails(
//      addNewFilingMember = None,
//      safeId = sub.filingMemberDetails.safeId,
//      customerIdentification1 = sub.filingMemberDetails.customerIdentification1,
//      customerIdentification2 = sub.filingMemberDetails.customerIdentification2,
//      organisationName = sub.filingMemberDetails.organisationName
//    )
//
//    val accountStatusOpt: Option[AccountStatus] = sub.accountStatus match {
//      case Some(status) if opType == 0 => Some(status)
//      case _ => None
//    }
//
//    val accountingPeriod = opType match {
//      case 0 => AccountingPeriod(
//        startDate = sub.accountingPeriod.startDate,
//        endDate = sub.accountingPeriod.endDate,
//        duetDate = sub.accountingPeriod.duetDate
//      )
//      case 1 => AccountingPeriod(
//        startDate = sub.accountingPeriod.startDate,
//        endDate = sub.accountingPeriod.endDate
//      )
//      case _ => throw new IllegalArgumentException("Invalid operation type")
//    }
//
////    val accountingPeriod = AccountingPeriod(
////      startDate = sub.accountingPeriod.startDate,
////      endDate = sub.accountingPeriod.endDate,
////      duetDate = sub.accountingPeriod.duetDate
////    )
//
//    val accountStatusOpt: Option[AccountStatus] = Some(sub.accountStatus)
//    val defaultAccountStatus = AccountStatus(inactive = true)
//    val result = for {
//
//      u1  <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
//      u2  <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
//      u3  <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
//      u4  <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
//      u5  <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
//      u6  <- u5.set(upeRegInformationId, registrationInfo)
//      u7  <- u6.set(upeRegisteredAddressId, ukAddress)
//      u8  <- u7.set(FmSafeId, sub.filingMemberDetails.safeId)
//      u9  <- u8.set(subFilingMemberDetailsId, filingMemberDetails)
//
//      userAnswers <- if (opType == 0) {
//        for {
//          u10 <- u9.set(subAccountingPeriodId, accountingPeriod)
//          u11 <- accountStatusOpt.fold(Future.successful(u10))(status => u10.set(subAccountStatusId, status))
//          telephone = sub.secondaryContactDetails.telepphone
//          telephoneStr = telephone.getOrElse("")
//          u12 <- u11.set(subSecondaryCapturePhoneId, telephoneStr)
//        } yield u12
//      } else {
//        for {
//          u10 <- u9.set(subAccountingPeriodId, accountingPeriod)
//          telephone = sub.secondaryContactDetails.telepphone
//          telephoneStr = telephone.getOrElse("")
//          u11 <- u10.set(subSecondaryCapturePhoneId, telephoneStr)
//        } yield u11
//      }
//
//
////      u10 <- u9.set(subAccountingPeriodId, accountingPeriod)
////      u11 <- u10.set(subAccountStatusId, accountStatusOpt.fold(defaultAccountStatus)(identity))
////      u12 <- u11.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
////      telephone: Option[String] = sub.secondaryContactDetails.telepphone
////      telephoneStr = telephone.getOrElse("")
////      u13 <- u12.set(subSecondaryCapturePhoneId, telephoneStr)
//    } yield userAnswers
//
//    result match {
//      case Success(userAnswers) =>
//        Future.successful(Json.toJson(userAnswers))
//      case Failure(exception) =>
//        logger.error("An error occurred while extracting subscription data", exception)
//        Future.successful(Json.obj("error" -> exception.getMessage))
//    }
//
//  }

  def extractAndProcess(json: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    json.validate[UserAnswers] match {
      case JsSuccess(userAnswers, _) =>
        val subscriptionResponse = constructSubscriptionResponse(userAnswers)

        subscriptionConnectors.amendSubscriptionInformation(subscriptionResponse).flatMap { response =>
          response.status match {
            case 200 =>
              extractSubscriptionData(userAnswers.id, subscriptionResponse.success, 1).flatMap(handleJsObjectResponse)

            case 409 =>
              Future.successful(HttpResponse(409, "Conflict error"))

            case 422 =>
              Future.successful(HttpResponse(422, "Unprocessable entity error"))

            case 500 =>
              Future.successful(HttpResponse(500, "Internal server error"))

            case 503 =>
              Future.successful(HttpResponse(503, "Service unavailable error"))

            case _ =>
              Future.successful(HttpResponse(response.status, "Failed to amend subscription information"))
          }
        }
      case JsError(errors) =>
        errors.foreach { case (path, validationErrors) =>
          logger.error(s"Validation error at path $path: ${validationErrors.mkString(", ")}")
        }
        Future.successful(HttpResponse(400, "Invalid JSON format"))
    }

  private def handleJsObjectResponse(jsValue: JsValue)(implicit ec: ExecutionContext): Future[HttpResponse] =
    jsValue match {
      case obj: JsObject =>
        obj.validate[UserAnswers] match {
          case JsSuccess(processedUserAnswers, _) =>
            repository.upsert(processedUserAnswers.id, processedUserAnswers.data).map { _ =>
              logger.info(s"Upserted user answers for id: ${processedUserAnswers.id}")
              HttpResponse(200, "Data processed and upserted successfully")
            }
          case JsError(upsertErrors) =>
            Future.successful(HttpResponse(400, "Failed to process subscription data"))
        }
      case _ =>
        Future.successful(HttpResponse(400, "Invalid data format"))
    }

  def constructSubscriptionResponse(userAnswers: UserAnswers): SubscriptionResponse = {
    val upeDetails               = userAnswers.data.\("upeDetails").as[UpeDetails]
    val accountingPeriod         = userAnswers.data.\("accountingPeriod").as[AccountingPeriod]
    val upeCorrespAddressDetails = userAnswers.data.\("upeCorrespAddressDetails").as[UpeCorrespAddressDetails]
    val primaryContactDetails    = userAnswers.data.\("primaryContactDetails").as[PrimaryContactDetails]
    val secondaryContactDetails  = userAnswers.data.\("secondaryContactDetails").as[SecondaryContactDetails]
    val filingMemberDetails      = userAnswers.data.\("filingMemberDetails").as[FilingMemberDetails]

    SubscriptionResponse(
      SubscriptionSuccess(
        plrReference = Some(upeDetails.plrReference.getOrElse("")),
        processingDate = Some(LocalDate.now()),
        formBundleNumber = None,
        upeDetails = upeDetails,
        upeCorrespAddressDetails = upeCorrespAddressDetails,
        primaryContactDetails = primaryContactDetails,
        secondaryContactDetails = secondaryContactDetails,
        filingMemberDetails = filingMemberDetails,
        accountingPeriod = accountingPeriod,
        accountStatus = AccountStatus(inactive = false)
      )
    )
  }
}
