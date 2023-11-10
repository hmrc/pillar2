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

  private def extractSubscriptionData(id: String, sub: SubscriptionSuccess): Future[JsValue] = {
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

    val accountingPeriod = AccountingPeriod(
      startDate = sub.accountingPeriod.startDate,
      endDate = sub.accountingPeriod.endDate,
      duetDate = sub.accountingPeriod.duetDate
    )

    val accountStatusOpt: Option[AccountStatus] = Some(sub.accountStatus)
    val defaultAccountStatus = AccountStatus(inactive = true)
    val result = for {

      u1  <- userAnswers.set(subMneOrDomesticId, if (sub.upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk)
      u2  <- u1.set(upeNameRegistrationId, sub.upeDetails.organisationName)
      u3  <- u2.set(subPrimaryContactNameId, sub.primaryContactDetails.name)
      u4  <- u3.set(subPrimaryEmailId, sub.primaryContactDetails.emailAddress)
      u5  <- u4.set(subSecondaryContactNameId, sub.secondaryContactDetails.name)
      u6  <- u5.set(upeRegInformationId, registrationInfo)
      u7  <- u6.set(upeRegisteredAddressId, ukAddress)
      u8  <- u7.set(FmSafeId, sub.filingMemberDetails.safeId)
      u9  <- u8.set(subFilingMemberDetailsId, filingMemberDetails)
      u10 <- u9.set(subAccountingPeriodId, accountingPeriod)
      u11 <- u10.set(subAccountStatusId, accountStatusOpt.fold(defaultAccountStatus)(identity))
      u12 <- u11.set(subSecondaryEmailId, sub.secondaryContactDetails.emailAddress)
      telephone: Option[String] = sub.secondaryContactDetails.telepphone
      telephoneStr = telephone.getOrElse("")
      u13 <- u12.set(subSecondaryCapturePhoneId, telephoneStr)
    } yield u13

    result match {
      case Success(userAnswers) =>
        Future.successful(Json.toJson(userAnswers))
      case Failure(exception) =>
        logger.error("An error occurred while extracting subscription data", exception)
        Future.successful(Json.obj("error" -> exception.getMessage))
    }

  }

  //  def extractAndProcess(json: JsValue): Future[HttpResponse] =
  def extractAndProcess(json: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    json.validate[UserAnswers] match {
      case JsSuccess(userAnswers, _) =>
        val extractedData = for {
          domesticOnly         <- userAnswers.get[Boolean](upeRegisteredInUKId)
          upeNameRegistration  <- userAnswers.get[String](upeNameRegistrationId)
          filingMember         <- userAnswers.get[Boolean](NominateFilingMemberId)
          primaryContactName   <- userAnswers.get[String](subPrimaryContactNameId)
          primaryEmail         <- userAnswers.get[String](subPrimaryEmailId)
          secondaryContactName <- userAnswers.get[String](subSecondaryContactNameId)
          regInformation       <- userAnswers.get[RegistrationInfo](upeRegInformationId)
          registeredAddress    <- userAnswers.get[UKAddress](upeRegisteredAddressId)
          safeId               <- userAnswers.get[String](FmSafeId)
          filingMemberDetails  <- userAnswers.get[FilingMemberDetails](subFilingMemberDetailsId)
          accountingPeriod     <- userAnswers.get[AccountingPeriod](subAccountingPeriodId)
          accountStatus        <- userAnswers.get[AccountStatus](subAccountStatusId)
          secondaryEmail       <- userAnswers.get[String](subSecondaryEmailId)
          secondaryPhone       <- userAnswers.get[String](subSecondaryCapturePhoneId)
        } yield SubscriptionResponse(
          success = SubscriptionSuccess(
            plrReference = Some("SamplePlrReference"),
            processingDate = Some(LocalDate.now()),
            formBundleNumber = None,
            upeDetails = UpeDetails(
              plrReference = None,
              safeId = Some(safeId),
              customerIdentification1 = Some(regInformation.utr),
              customerIdentification2 = Some(regInformation.crn),
              organisationName = upeNameRegistration,
              registrationDate = regInformation.registrationDate.getOrElse(LocalDate.now),
              domesticOnly = domesticOnly,
              filingMember = filingMember
            ),
            upeCorrespAddressDetails = UpeCorrespAddressDetails(
              addressLine1 = registeredAddress.addressLine1,
              addressLine2 = registeredAddress.addressLine2,
              addressLine3 = Some(registeredAddress.addressLine3),
              addressLine4 = registeredAddress.addressLine4,
              postCode = Some(registeredAddress.postalCode),
              countryCode = registeredAddress.countryCode
            ),
            primaryContactDetails = PrimaryContactDetails(
              name = primaryContactName,
              telepphone = None,
              emailAddress = primaryEmail
            ),
            secondaryContactDetails = SecondaryContactDetails(
              name = secondaryContactName,
              telepphone = Some(secondaryPhone),
              emailAddress = secondaryEmail
            ),
            filingMemberDetails = filingMemberDetails,
            accountingPeriod = accountingPeriod,
            accountStatus = accountStatus
          )
        )

        extractedData match {
          case Some(subscriptionResponse) =>
            subscriptionConnectors.amendSubscriptionInformation(subscriptionResponse).flatMap { response =>
              if (response.status == 200) {
                repository.upsert(userAnswers.id, userAnswers.data).map { _ =>
                  logger.info(s"Upserted user answers for id: ${userAnswers.id}")
                  HttpResponse(200, "Data processed and upserted successfully")
                }
              } else {
                Future.successful(HttpResponse(response.status, "Failed to amend subscription information"))
              }
            }

          case None =>
            Future.successful(HttpResponse(400, "Failed to extract data"))
        }

      case _ =>
        Future.successful(HttpResponse(400, "Invalid JSON format"))
    }
}
