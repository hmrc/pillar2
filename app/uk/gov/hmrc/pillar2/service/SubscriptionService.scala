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
import uk.gov.hmrc.pillar2.models.hods.subscription.common.{ContactDetailsType, UpeCorrespAddressDetails, _}
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail
import uk.gov.hmrc.pillar2.models.identifiers._
import uk.gov.hmrc.pillar2.models.registration.GrsResponse
import uk.gov.hmrc.pillar2.models.subscription.{ExtraSubscription, MneOrDomestic}
import uk.gov.hmrc.pillar2.models.{AccountStatus, AccountingPeriod, NonUKAddress, UserAnswers}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions

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

  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
    val userAnswers = UserAnswers(id, Json.obj())

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

    val extraSubscription = ExtraSubscription(
      formBundleNumber = Some(getNonEmptyOrNA(sub.formBundleNumber)),
      crn = crn.map(getNonEmptyOrNA),
      utr = utr.map(getNonEmptyOrNA),
      safeId = safeId.map(getNonEmptyOrNA)
    )

    val filingMemberDetails = FilingMemberDetails(
      safeId = sub.filingMemberDetails.safeId,
      customerIdentification1 = sub.filingMemberDetails.customerIdentification1,
      customerIdentification2 = sub.filingMemberDetails.customerIdentification2,
      organisationName = sub.filingMemberDetails.organisationName
    )

    val accountingPeriod = AccountingPeriod(
      startDate = sub.accountingPeriod.startDate,
      endDate = sub.accountingPeriod.endDate,
      duetDate = sub.accountingPeriod.duetDate
    )

    val accountStatus = AccountStatus(
      inactive = sub.accountStatus.inactive
    )

    val primaryHasTelephone:   Boolean = sub.primaryContactDetails.telepphone.isDefined
    val secondaryHasTelephone: Boolean = sub.secondaryContactDetails.telepphone.isDefined
    val hasSecondaryContactData: Boolean = sub.secondaryContactDetails.telepphone.exists(
      _.nonEmpty
    ) || sub.secondaryContactDetails.emailAddress.nonEmpty || sub.secondaryContactDetails.name.nonEmpty

    val result = for {
      u1  <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
      u2  <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
      u3  <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
      u4  <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
      u5  <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
      u6  <- u5.set(subRegisteredAddressId, nonUKAddress)
      u7  <- u6.set(FmSafeId, sub.filingMemberDetails.safeId)
      u8  <- u7.set(subFilingMemberDetailsId, filingMemberDetails)
      u9  <- u8.set(subAccountingPeriodId, accountingPeriod)
      u10 <- u9.set(subAccountStatusId, accountStatus)
      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.filingMember)
      telephone: Option[String] = sub.secondaryContactDetails.telepphone
      u13 <- u12.set(subSecondaryCapturePhoneId, getOrEmptyString(telephone))
      u14 <- u13.set(subExtraSubscriptionId, extraSubscription)
      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.registrationDate)
      u16 <- u15.set(fmDashboardId, dashboardInfo)
      phone: Option[String] = sub.primaryContactDetails.telepphone
      u17 <- u16.set(subPrimaryCapturePhoneId, getOrEmptyString(phone))
      u18 <- u17.set(subPrimaryPhonePreferenceId, primaryHasTelephone)
      u19 <- u18.set(subSecondaryPhonePreferenceId, secondaryHasTelephone)
      u20 <- u19.set(subAddSecondaryContactId, hasSecondaryContactData)
    } yield u20

    result match {
      case Success(userAnswers) =>
        Future.successful(Json.toJson(userAnswers))
      case Failure(exception) =>
        logger.error("An error occurred while extracting subscription data", exception)
        Future.successful(Json.obj("error" -> exception.getMessage))
    }

  }

//  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    val dashboardInfo = sub.upeDetails match {
//      case Some(upeDetails) =>
//        DashboardInfo(
//          organisationName = upeDetails.organisationName,
//          registrationDate = upeDetails.registrationDate
//        )
//      case None =>
//        DashboardInfo(
//          organisationName = "N/A",
//          registrationDate = LocalDate.now()
//        )
//    }
//
//    val nonUKAddressOpt = sub.upeCorrespAddressDetails.map { addressDetails =>
//      NonUKAddress(
//        addressLine1 = if (addressDetails.addressLine1.nonEmpty) addressDetails.addressLine1 else "N/A",
//        addressLine2 = addressDetails.addressLine2,
//        addressLine3 = addressDetails.addressLine3 match {
//          case Some(line) if line.nonEmpty => line
//          case _                           => "N/A"
//        },
//        addressLine4 = addressDetails.addressLine4,
//        postalCode = addressDetails.postCode,
//        countryCode = addressDetails.countryCode
//      )
//    }
//
//    val extraSubscriptionOpt = sub.upeDetails.map { details =>
//      ExtraSubscription(
//        formBundleNumber = sub.formBundleNumber,
//        crn = details.customerIdentification1,
//        utr = details.customerIdentification2,
//        safeId = details.safeId
//      )
//    }
//
//    val filingMemberDetails = sub.filingMemberDetails match {
//      case Some(details) =>
//        FilingMemberDetails(
//          safeId = details.safeId,
//          customerIdentification1 = details.customerIdentification1,
//          customerIdentification2 = details.customerIdentification2,
//          organisationName = details.organisationName
//        )
//      case None =>
//        FilingMemberDetails(
//          safeId = "DefaultSafeId",
//          customerIdentification1 = Some("DefaultID1"),
//          customerIdentification2 = Some("DefaultID2"),
//          organisationName = "DefaultOrgName"
//        )
//    }
//
//    val filingMemberDetailsOpt = sub.filingMemberDetails.map(identity)
//    val accountingPeriodOpt    = sub.accountingPeriod.map(identity)
//    val accountStatusOpt       = sub.accountStatus.map(identity)
//
//    val primaryHasTelephone:   Boolean = sub.primaryContactDetails.flatMap(_.telepphone).isDefined
//    val secondaryHasTelephone: Boolean = sub.secondaryContactDetails.flatMap(_.telepphone).isDefined
//    val hasSecondaryContactData: Boolean = sub.secondaryContactDetails.map(_.telepphone).exists(_.nonEmpty) ||
//      sub.secondaryContactDetails.map(_.emailAddress).exists(_.nonEmpty) ||
//      sub.secondaryContactDetails.map(_.name).exists(_.nonEmpty)
//
////    val primaryContactDetailsEmailAddress = sub.primaryContactDetails.map(_.emailAddress)
//
////    sub.secondaryContactDetails.name
//
//
////    val result = for {
////      u1 <- userAnswers.set(subMneOrDomesticId, sub.upeDetails.map(_.domesticOnly).fold(MneOrDomestic.uk)(domesticOnly => if (domesticOnly) MneOrDomestic.ukAndOther else MneOrDomestic.uk))
////      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.map(_.organisationName) match {
////        case Some(orgName) => orgName
////        case None => "N/A"
////      })
////      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.map(_.name) match {
////        case Some(contactName) => contactName
////        case None => "N/A"
////      })
////      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.map(_.emailAddress) match {
////        case Some(email) => email
////        case None => "N/A"
////      })
////
////
////      u4 <- u3.set(subPrimaryEmailId, primaryContactDetailsEmailAddress)
////
////      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
////      u6 <- u5.set(subRegisteredAddressId, nonUKAddress)
////      u7 <- u6.set(FmSafeId, sub.filingMemberDetails.safeId)
////      u8 <- u7.set(subFilingMemberDetailsId, filingMemberDetails)
////      u9 <- u8.set(subAccountingPeriodId, accountingPeriod)
////      u10 <- u9.set(subAccountStatusId, accountStatus)
////      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
////      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.filingMember)
////      telephone: Option[String] = sub.secondaryContactDetails.telepphone
////      u13 <- u12.set(subSecondaryCapturePhoneId, getOrEmptyString(telephone))
////      u14 <- u13.set(subExtraSubscriptionId, extraSubscription)
////      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.registrationDate)
////      u16 <- u15.set(fmDashboardId, dashboardInfo)
////      phone: Option[String] = sub.primaryContactDetails.telepphone
////      u17 <- u16.set(subPrimaryCapturePhoneId, getOrEmptyString(phone))
////      u18 <- u17.set(subPrimaryPhonePreferenceId, primaryHasTelephone)
////      u19 <- u18.set(subSecondaryPhonePreferenceId, secondaryHasTelephone)
////      u20 <- u19.set(subAddSecondaryContactId, hasSecondaryContactData)
////    } yield u20
//
//    val organisationName: String = sub.upeDetails match {
//      case Some(upeDetails) => upeDetails.organisationName
//      case None => ""
//    }
//
//    val primaryContactName: String = sub.primaryContactDetails match {
//      case Some(contactDetails) => contactDetails.name
//      case None => ""
//    }
//
//    val primaryContactEmailAddress: String = sub.primaryContactDetails match {
//      case Some(contactDetails) => contactDetails.emailAddress
//      case None => ""
//    }
//
//    val secondaryContactName: String = sub.secondaryContactDetails match {
//      case Some(contactDetails) => contactDetails.name
//      case None => ""
//    }
//
//
//    import uk.gov.hmrc.pillar2.models.NonUKAddress.format
//
//
//
//
//    val result = for {
//      u1 <- userAnswers.set(
//              subMneOrDomesticId,
//              sub.upeDetails
//                .map(_.domesticOnly)
//                .fold(MneOrDomestic.uk)(domesticOnly => if (domesticOnly) MneOrDomestic.ukAndOther else MneOrDomestic.uk)
//            )
//
//      u2 <- u1.set(upeNameRegistrationId, organisationName)
//      u3 <- u2.set(subPrimaryContactNameId, primaryContactName)
//
//      u4 <- u3.set(subPrimaryEmailId, primaryContactEmailAddress)
//      u5 <- u4.set(subSecondaryContactNameId, secondaryContactName)
//      u6  <- u5.set(subRegisteredAddressId, nonUKAddressOpt)
//      u7  <- u6.set(FmSafeId, sub.filingMemberDetails.safeId)
//      u8  <- u7.set(subFilingMemberDetailsId, filingMemberDetails)
//      u9  <- u8.set(subAccountingPeriodId, accountingPeriod)
//      u10 <- u9.set(subAccountStatusId, accountStatus)
//      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
//      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.filingMember)
//      telephone: Option[String] = sub.secondaryContactDetails.telepphone
//      u13 <- u12.set(subSecondaryCapturePhoneId, getOrEmptyString(telephone))
//      u14 <- u13.set(subExtraSubscriptionId, extraSubscription)
//      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.registrationDate)
//      u16 <- u15.set(fmDashboardId, dashboardInfo)
//      phone: Option[String] = sub.primaryContactDetails.telepphone
//      u17 <- u16.set(subPrimaryCapturePhoneId, getOrEmptyString(phone))
//      u18 <- u17.set(subPrimaryPhonePreferenceId, primaryHasTelephone)
//      u19 <- u18.set(subSecondaryPhonePreferenceId, secondaryHasTelephone)
//      u20 <- u19.set(subAddSecondaryContactId, hasSecondaryContactData)
//    } yield u20
//
//    /*
//    val result = for {
//      u1 <- userAnswers.set(subMneOrDomesticId, sub.upeDetails.map(_.domesticOnly).fold(MneOrDomestic.uk)(domesticOnly => if (domesticOnly) MneOrDomestic.ukAndOther else MneOrDomestic.uk))
//      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.map(_.organisationName) match {
//        case Some(orgName) => orgName
//        case None => "N/A"
//      })
//      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.map(_.name) match {
//        case Some(contactName) => contactName
//        case None => "N/A"
//      })
//      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.map(_.emailAddress) match {
//        case Some(email) => email
//        case None => "N/A"
//      })
//      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.map(_.name) match {
//        case Some(contactName) => contactName
//        case None => "N/A"
//      })
//      u6 <- u5.set(subRegisteredAddressId, nonUKAddressOpt match {
//        case Some(address) => address.
//        case None => NonUKAddress.default // Assuming there's a default value defined in NonUKAddress
//      })
////      u6  <- u5.set(subRegisteredAddressId, nonUKAddressOpt.fold(NonUKAddress.default)(identity))
//      u7  <- u6.set(FmSafeId, sub.filingMemberDetails.flatMap(_.safeId).fold("N/A")(identity))
//      u8  <- u7.set(subFilingMemberDetailsId, filingMemberDetailsOpt.fold(FilingMemberDetails.default)(identity))
//      u9  <- u8.set(subAccountingPeriodId, accountingPeriodOpt.fold(AccountingPeriod.default)(identity))
//      u10 <- u9.set(subAccountStatusId, accountStatusOpt.fold(AccountStatus.default)(identity))
//      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.flatMap(_.emailAddress).fold("N/A")(identity))
//      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.flatMap(_.filingMember).fold("N/A")(identity))
//      telephone = sub.secondaryContactDetails.flatMap(_.telepphone).fold("N/A")(identity)
//      u13 <- u12.set(subSecondaryCapturePhoneId, telephone)
//      u14 <- u13.set(subExtraSubscriptionId, extraSubscriptionOpt.fold(ExtraSubscription.default)(identity))
//      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.flatMap(_.registrationDate).fold(LocalDate.now)(identity))
//    } yield u15
//     */
//    result match {
//      case Success(userAnswers) =>
//        Future.successful(Json.toJson(userAnswers))
//      case Failure(exception) =>
//        logger.error("An error occurred while extracting subscription data", exception)
//        Future.successful(Json.obj("error" -> exception.getMessage))
//    }
//  }

  //  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    val nonUKAddressOpt = sub.upeCorrespAddressDetails.map { addressDetails =>
//      NonUKAddress(
//        addressLine1 = addressDetails.addressLine1,
//        addressLine2 = addressDetails.addressLine2.fold("N/A")(line => if (line.nonEmpty) line else "N/A"),
//        addressLine3 = addressDetails.addressLine3.fold("N/A")(line => if (line.nonEmpty) line else "N/A"),
//        addressLine4 = addressDetails.addressLine4.fold("N/A")(line => if (line.nonEmpty) line else "N/A"),
//        postalCode = addressDetails.postCode.fold("N/A")(code => if (code.nonEmpty) code else "N/A"),
//        countryCode = addressDetails.countryCode
//      )
//    }
//
//    val extraSubscriptionOpt = sub.upeDetails.map { details =>
//      ExtraSubscription(
//        formBundleNumber = sub.formBundleNumber.fold("N/A")(identity),
//        crn = details.customerIdentification1.fold("N/A")(identity),
//        utr = details.customerIdentification2.fold("N/A")(identity),
//        safeId = details.safeId.fold("N/A")(identity)
//      )
//    }
//
//    val filingMemberDetailsOpt = sub.filingMemberDetails.map(identity)
//    val accountingPeriodOpt = sub.accountingPeriod.map(identity)
//    val accountStatusOpt = sub.accountStatus.map(identity)
//
//    val result = for {
//      u1 <- userAnswers.set(subMneOrDomesticId, sub.upeDetails.flatMap(_.domesticOnly).fold(MneOrDomestic.Uk)(_ => MneOrDomestic.UkAndOther))
//      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.flatMap(_.organisationName).fold("N/A")(identity))
//      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.flatMap(_.name).fold("N/A")(identity))
//      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.flatMap(_.emailAddress).fold("N/A")(identity))
//      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.flatMap(_.name).fold("N/A")(identity))
//      u6 <- u5.set(subRegisteredAddressId, nonUKAddressOpt.fold(NonUKAddress.default)(identity))
//      u7 <- u6.set(FmSafeId, sub.filingMemberDetails.flatMap(_.safeId).fold("N/A")(identity))
//      u8 <- u7.set(subFilingMemberDetailsId, filingMemberDetailsOpt.fold(FilingMemberDetails.default)(identity))
//      u9 <- u8.set(subAccountingPeriodId, accountingPeriodOpt.fold(AccountingPeriod.default)(identity))
//      u10 <- u9.set(subAccountStatusId, accountStatusOpt.fold(AccountStatus.default)(identity))
//      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.flatMap(_.emailAddress).fold("N/A")(identity))
//      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.flatMap(_.filingMember).fold("N/A")(identity))
//      telephone = sub.secondaryContactDetails.flatMap(_.telepphone).fold("N/A")(identity)
//      u13 <- u12.set(subSecondaryCapturePhoneId, telephone)
//      u14 <- u13.set(subExtraSubscriptionId, extraSubscriptionOpt.fold(ExtraSubscription.default)(identity))
//      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.flatMap(_.registrationDate).fold(LocalDate.now)(identity))
//    } yield u15
//
//    result match {
//      case Success(userAnswers) =>
//        Future.successful(Json.toJson(userAnswers))
//      case Failure(exception) =>
//        logger.error("An error occurred while extracting subscription data", exception)
//        Future.successful(Json.obj("error" -> exception.getMessage))
//    }
//  }

  //  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    val nonUKAddressOpt = sub.upeCorrespAddressDetails.map { upeCorrespAddressDetails =>
//      NonUKAddress(
//        addressLine1 = upeCorrespAddressDetails.addressLine1,
//        addressLine2 = upeCorrespAddressDetails.addressLine2.filter(_.nonEmpty).orElse(Some("N/A")),
//        addressLine3 = upeCorrespAddressDetails.addressLine3 match {
//          case Some(str) if str.nonEmpty => str
//          case _                         => "N/A"
//        },
//        addressLine4 = upeCorrespAddressDetails.addressLine4.filter(_.nonEmpty).orElse(Some("N/A")),
//        postalCode = upeCorrespAddressDetails.postCode.filter(_.nonEmpty).orElse(Some("N/A")),
//        countryCode = upeCorrespAddressDetails.countryCode
//      )
//    }
//
//    val extraSubscriptionOpt = sub.upeDetails.map { upeDetails =>
//      ExtraSubscription(
//        formBundleNumber = sub.formBundleNumber.map(getNonEmptyOrNA).orElse(Some("N/A")),
//        crn = upeDetails.customerIdentification1.map(getNonEmptyOrNA).orElse(Some("N/A")),
//        utr = upeDetails.customerIdentification2.map(getNonEmptyOrNA).orElse(Some("N/A")),
//        safeId = upeDetails.safeId.map(getNonEmptyOrNA).orElse(Some("N/A"))
//      )
//    }
//
//    val filingMemberDetailsOpt = sub.filingMemberDetails.map { filingMemberDetails =>
//      FilingMemberDetails(
//        safeId = filingMemberDetails.safeId,
//        customerIdentification1 = filingMemberDetails.customerIdentification1,
//        customerIdentification2 = filingMemberDetails.customerIdentification2,
//        organisationName = filingMemberDetails.organisationName
//      )
//    }
//
//    val accountStatusOpt = sub.accountStatus.map { accountStatus =>
//      AccountStatus(
//        inactive = accountStatus.inactive
//      )
//    }
//
//
//    val accountingPeriodOpt = sub.accountingPeriod.map { accountingPeriod =>
//      AccountingPeriod(
//        startDate = accountingPeriod.startDate,
//        endDate = accountingPeriod.endDate,
//        duetDate = accountingPeriod.duetDate
//      )
//    }
//
//    val result = for {
//      u1 <- userAnswers.set(subMneOrDomesticId, sub.upeDetails.flatMap(_.domesticOnly).fold(MneOrDomestic.Uk)(if (_) MneOrDomestic.UkAndOther else MneOrDomestic.Uk))
//      u2 <- u1.set(upeNameRegistrationId, sub.upeDetails.flatMap(_.organisationName).fold("N/A")(identity))
//      u3 <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.flatMap(_.name).fold("N/A")(identity))
//      u4 <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.flatMap(_.emailAddress).fold("N/A")(identity))
//      u5 <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.flatMap(_.name).fold("N/A")(identity))
//      u6 <- u5.set(subRegisteredAddressId, nonUKAddressOpt.fold[NonUKAddress](NonUKAddress.default)(identity)) // Assuming NonUKAddress.default exists
//      u7 <- u6.set(FmSafeId, sub.filingMemberDetails.flatMap(_.safeId).fold("N/A")(identity))
//      u8 <- u7.set(subFilingMemberDetailsId, filingMemberDetailsOpt.fold(FilingMemberDetails.default)(identity)) // Assuming FilingMemberDetails.default exists
//      u9 <- u8.set(subAccountingPeriodId, accountingPeriodOpt.fold(AccountingPeriod.default)(identity)) // Assuming AccountingPeriod.default exists
//      u10 <- u9.set(subAccountStatusId, accountStatusOpt.fold(AccountStatus.default)(identity)) // Assuming AccountStatus.default exists
//      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.flatMap(_.emailAddress).fold("N/A")(identity))
//      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.flatMap(_.filingMember).fold("N/A")(identity))
//      telephone = sub.secondaryContactDetails.flatMap(_.telepphone).fold("N/A")(identity)
//      u13 <- u12.set(subSecondaryCapturePhoneId, telephone)
//      u14 <- u13.set(subExtraSubscriptionId, extraSubscriptionOpt.fold(ExtraSubscription.default)(identity)) // Assuming ExtraSubscription.default exists
//      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.flatMap(_.registrationDate).fold(LocalDate.now)(identity)) // Use current date as default
//    } yield u15
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

  def getOrEmptyString[T](option: Option[T]): String = option match {
    case Some(value) if !value.toString.isEmpty => value.toString
    case _                                      => "N/A"
  }

  def getNonEmptyOrNA(value: String): String =
    if (value.nonEmpty) value else "N/A"

//  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
//    val userAnswers = UserAnswers(id, Json.obj())
//
//    /*
//
//    val dashboardInfoOpt = sub.upeDetails.map { upeDetails =>
//
//      val dashboardInfo = DashboardInfo(
//        organisationName = upeDetails.organisationName,
//        registrationDate = upeDetails.registrationDate
//      )
//    }
//
//    val nonUKAddressOpt = sub.upeCorrespAddressDetails.map { upeCorrespAddressDetails =>
//
//      val nonUKAddress = NonUKAddress(
//        addressLine1 = upeCorrespAddressDetails.addressLine1,
//        addressLine2 = upeCorrespAddressDetails.addressLine2.filter(_.nonEmpty).orElse(Some("N/A")),
//        addressLine3 = upeCorrespAddressDetails.addressLine3 match {
//          case Some(str) if str.nonEmpty => str
//          case _ => "N/A"
//        },
//        addressLine4 = upeCorrespAddressDetails.addressLine4.filter(_.nonEmpty).orElse(Some("N/A")),
//        postalCode = upeCorrespAddressDetails.postCode.filter(_.nonEmpty).orElse(Some("N/A")),
//        countryCode = upeCorrespAddressDetails.countryCode
//      )
//    }
//
//    val crnsOpt = sub.upeDetails.map { upeDetails =>
//      val crn = upeDetails.customerIdentification1
//    }
//
//    val utrOpt = sub.upeDetails.map { upeDetails =>
//      val utr = upeDetails.customerIdentification2
//    }
//
//    val safeIdOpt = sub.upeDetails.map { upeDetails =>
//      val safeId = upeDetails.safeId
//    }
//
//
//    val extraSubscription = ExtraSubscription(
//          formBundleNumber = Some(getNonEmptyOrNA(sub.formBundleNumber)),
//          crn = crnsOpt.map(getNonEmptyOrNA),
//          utr = utrOpt.map(getNonEmptyOrNA),
//          safeId = safeIdOpt.map(getNonEmptyOrNA)
//        )
//
//    val filingMemberDetails = FilingMemberDetails(
//      safeId = sub.filingMemberDetails.safeId,
//      customerIdentification1 = sub.filingMemberDetails.customerIdentification1,
//      customerIdentification2 = sub.filingMemberDetails.customerIdentification2,
//      organisationName = sub.filingMemberDetails.organisationName
//    )
//
//    val accountingPeriod = AccountingPeriod(
//      startDate = sub.accountingPeriod.startDate,
//      endDate = sub.accountingPeriod.endDate,
//      duetDate = sub.accountingPeriod.duetDate
//    )
//
//    val accountStatus = AccountStatus(
//      inactive = sub.accountStatus.inactive
//    )
//
//    val primaryHasTelephone:   Boolean = sub.primaryContactDetails.telepphone.isDefined
//    val secondaryHasTelephone: Boolean = sub.secondaryContactDetails.telepphone.isDefined
//    val hasSecondaryContactData: Boolean = sub.secondaryContactDetails.telepphone.exists(
//      _.nonEmpty
//    ) || sub.secondaryContactDetails.emailAddress.nonEmpty || sub.secondaryContactDetails.name.nonEmpty
//    */
//
//
//    val dashboardInfo = sub.upeDetails.fold(DashboardInfo("", LocalDate.now())) { upeDetails =>
//      DashboardInfo(
//        organisationName = upeDetails.organisationName,
//        registrationDate = upeDetails.registrationDate
//      )
//    }
//
//    // Similar approach for nonUKAddress
//    val nonUKAddress = sub.upeCorrespAddressDetails.fold(NonUKAddress("", None, "", None, None, "")) { addr =>
//      NonUKAddress(
//        addressLine1 = addr.addressLine1,
//        addressLine2 = addr.addressLine2.orElse(Some("N/A")),
//        addressLine3 = addr.addressLine3.orElse(Some("N/A")),
//        addressLine4 = addr.addressLine4.orElse(Some("N/A")),
//        postalCode = addr.postCode.orElse(Some("N/A")),
//        countryCode = addr.countryCode
//      )
//    }
//
//    // Folding over the optional ExtraSubscription values
//    val formBundleNumber = sub.formBundleNumber.fold("N/A")(identity)
//    val crn = sub.upeDetails.flatMap(_.customerIdentification1).orElse(Some("N/A"))
//    val utr = sub.upeDetails.flatMap(_.customerIdentification2).orElse(Some("N/A"))
//    val safeId = sub.upeDetails.flatMap(_.safeId).orElse(Some("N/A"))
//
//    val extraSubscription = ExtraSubscription(formBundleNumber, crn, utr, safeId)
//
//    val filingMemberDetails = sub.filingMemberDetails.fold(FilingMemberDetails("", None, None, ""))(identity)
//    val accountingPeriod = sub.accountingPeriod.fold(AccountingPeriod(LocalDate.now(), LocalDate.now()))(identity)
//    val accountStatus = sub.accountStatus.fold(AccountStatus(false))(identity)
//
//    val primaryHasTelephone = sub.primaryContactDetails.flatMap(_.telephone).isDefined
//    val secondaryHasTelephone = sub.secondaryContactDetails.flatMap(_.telephone).isDefined
//    val hasSecondaryContactData = sub.secondaryContactDetails.exists(details =>
//      details.telephone.nonEmpty || details.emailAddress.nonEmpty || details.name.nonEmpty
//    )
//
//    val result = for {
//      u1  <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
//      u2  <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
//      u3  <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
//      u4  <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
//      u5  <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
//      u6  <- u5.set(subRegisteredAddressId, nonUKAddress)
//      u7  <- u6.set(FmSafeId, sub.filingMemberDetails.safeId)
//      u8  <- u7.set(subFilingMemberDetailsId, filingMemberDetails)
//      u9  <- u8.set(subAccountingPeriodId, accountingPeriod)
//      u10 <- u9.set(subAccountStatusId, accountStatus)
//      u11 <- u10.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
//      u12 <- u11.set(NominateFilingMemberId, sub.upeDetails.filingMember)
//      telephone: Option[String] = sub.secondaryContactDetails.telepphone
//      u13 <- u12.set(subSecondaryCapturePhoneId, getOrEmptyString(telephone))
//      u14 <- u13.set(subExtraSubscriptionId, extraSubscription)
//      u15 <- u14.set(subRegistrationDateId, sub.upeDetails.registrationDate)
//      u16 <- u15.set(fmDashboardId, dashboardInfo)
//      phone: Option[String] = sub.primaryContactDetails.telepphone
//      u17 <- u16.set(subPrimaryCapturePhoneId, getOrEmptyString(phone))
//      u18 <- u17.set(subPrimaryPhonePreferenceId, primaryHasTelephone)
//      u19 <- u18.set(subSecondaryPhonePreferenceId, secondaryHasTelephone)
//      u20 <- u19.set(subAddSecondaryContactId, hasSecondaryContactData)
//    } yield u20
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

//  def extractAndProcess(json: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
//    json.validate[UserAnswers] match {
//      case JsSuccess(userAnswers, _) =>
//        val subscriptionResponse = constructSubscriptionResponse(userAnswers)
//
//        subscriptionConnectors.amendSubscriptionInformation(subscriptionResponse).map { response =>
//
//            case 200 =>
//
//
//            case 409 =>
//              Future.successful(HttpResponse(409, "Conflict error"))
//
//            case 422 =>
//              Future.successful(HttpResponse(422, "Unprocessable entity error"))
//
//            case 500 =>
//              Future.successful(HttpResponse(500, "Internal server error"))
//
//            case 503 =>
//              Future.successful(HttpResponse(503, "Service unavailable error"))
//
//            case _ =>
//              Future.successful(HttpResponse(response.status, "Failed to amend subscription information"))
//          }
//        }
//      case JsError(errors) =>
//        errors.foreach { case (path, validationErrors) =>
//          logger.error(s"Validation error at path $path: ${validationErrors.mkString(", ")}")
//        }
//        Future.successful(HttpResponse(400, "Invalid JSON format"))
//    }

//  def extractAndProcess(json: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
//    json.validate[UserAnswers] match {
//      case JsSuccess(userAnswers, _) =>
//        constructSubscriptionResponse(userAnswers) match {
//          case Some(subscriptionResponse) =>
//            subscriptionConnectors.amendSubscriptionInformation(subscriptionResponse).flatMap { response =>
//              response.status match {
//                case 200 =>
//                  Future.successful(HttpResponse(200, "Amendment successful"))
//                case status =>
//                  Future.successful(HttpResponse(status, s"Error with status: $status"))
//              }
//            }
//          case None =>
//            Future.successful(HttpResponse(400, "Invalid subscription response"))
//        }
//      case JsError(errors) =>
//        val errorMessage = errors
//          .map { case (path, validationErrors) =>
//            s"$path: ${validationErrors.map(_.message).mkString(", ")}"
//          }
//          .mkString("; ")
//        logger.error(s"Validation error: $errorMessage")
//        Future.successful(HttpResponse(400, "Invalid JSON format"))
//    }

  def extractAndProcess(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    constructSubscriptionResponse(userAnswers) match {
      case Some(subscriptionResponse) =>
        subscriptionConnectors.amendSubscriptionInformation(subscriptionResponse).flatMap { response =>
          response.status match {
            case 200 =>
              Future.successful(HttpResponse(200, "Amendment successful"))

            case status =>
              Future.successful(HttpResponse(status, s"Error with status: $status"))
          }
        }

      case None =>
        Future.successful(HttpResponse(400, "Invalid subscription response"))
    }

  def constructSubscriptionResponse(userAnswers: UserAnswers): Option[AmendSubscriptionResponse] =
    userAnswers.get(subRegisteredAddressId).flatMap { nonUKAddress =>
      for {
        domesticOnly                        <- userAnswers.get(subMneOrDomesticId)
        upeDetailsOrganisationName          <- userAnswers.get(upeNameRegistrationId)
        primaryContactDetailsName           <- userAnswers.get(subPrimaryContactNameId)
        primaryContactDetailsEmailAddress   <- userAnswers.get(subPrimaryEmailId)
        secondaryContactDetailsName         <- userAnswers.get(subSecondaryContactNameId)
        secondaryContactDetailsEmailAddress <- userAnswers.get(subSecondaryEmailId)
        safeId                              <- userAnswers.get(FmSafeId)
        filingMemberDetails                 <- userAnswers.get(subFilingMemberDetailsId)
        accountingPeriod                    <- userAnswers.get(subAccountingPeriodId)
        accountStatus                       <- userAnswers.get(subAccountStatusId)
        upeDetailsFilingMember              <- userAnswers.get(NominateFilingMemberId)
        extraSubscription                   <- userAnswers.get(subExtraSubscriptionId)
        registrationDate                    <- userAnswers.get(subRegistrationDateId)
      } yield {
        val formBundleNumber = extraSubscription.formBundleNumber match {
          case Some(fbNumber) => fbNumber
          case None           => "N/A"
        }

        val customerIdentification1 = extraSubscription.crn
        val customerIdentification2 = extraSubscription.utr

        val upeDetails = UpeDetails(
          plrReference = extraSubscription.plrReference,
          safeId = None,
          customerIdentification1 = customerIdentification1,
          customerIdentification2 = customerIdentification2,
          organisationName = upeDetailsOrganisationName,
          registrationDate = registrationDate,
          domesticOnly = if (domesticOnly == MneOrDomestic.UkAndOther) true else false,
          filingMember = upeDetailsFilingMember
        )

        val primaryContactDetails = PrimaryContactDetails(
          name = primaryContactDetailsName,
          telepphone = None,
          telephone = userAnswers.get(subPrimaryCapturePhoneId),
          emailAddress = primaryContactDetailsEmailAddress
        )

        val secondaryContactDetails = SecondaryContactDetails(
          name = secondaryContactDetailsName,
          telepphone = None,
          telephone = userAnswers.get(subSecondaryCapturePhoneId),
          emailAddress = secondaryContactDetailsEmailAddress
        )

        val upeCorrespAddressDetails = UpeCorrespAddressDetails(
          addressLine1 = nonUKAddress.addressLine1,
          addressLine2 = nonUKAddress.addressLine2,
          addressLine3 = Some(nonUKAddress.addressLine3),
          addressLine4 = nonUKAddress.addressLine4,
          postCode = nonUKAddress.postalCode,
          countryCode = nonUKAddress.countryCode
        )

        val startDate: LocalDate = accountingPeriod.startDate
        val endDate:   LocalDate = accountingPeriod.endDate

        val accountingPeriodOpt = AccountingPeriod(
          startDate = startDate,
          endDate = endDate
        )

        AmendSubscriptionResponse(
          AmendSubscriptionSuccess(
            upeDetails = upeDetails,
            accountingPeriod = accountingPeriodOpt,
            upeCorrespAddressDetails = upeCorrespAddressDetails,
            primaryContactDetails = primaryContactDetails,
            secondaryContactDetails = secondaryContactDetails,
            filingMemberDetails = filingMemberDetails
          )
        )
      }
    }

}
