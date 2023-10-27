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
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.connectors.SubscriptionConnector
import uk.gov.hmrc.pillar2.models.fm.FilingMember
import uk.gov.hmrc.pillar2.models.grs.EntityType
import uk.gov.hmrc.pillar2.models.hods.subscription.common._
import uk.gov.hmrc.pillar2.models.hods.subscription.request.{CreateSubscriptionRequest, RequestDetail, SubscriptionRequest}
import uk.gov.hmrc.pillar2.models.identifiers.{FilingMemberId, RegistrationId, SubscriptionId}
import uk.gov.hmrc.pillar2.models.registration.Registration
import uk.gov.hmrc.pillar2.models.subscription.{MneOrDomestic, Subscription, SubscriptionAddress}
import uk.gov.hmrc.pillar2.models.{AccountingPeriod, _}
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
      upe  <- userAnswers.get(RegistrationId)
      fm   <- userAnswers.get(FilingMemberId)
      subs <- userAnswers.get(SubscriptionId)

    } yield {
      val subscriptionRequest = CreateSubscriptionRequest(
        createSubscriptionRequest = SubscriptionRequest(
          requestBody = RequestDetail(
            getUpeDetails(upeSafeId, upe, fm, subs),
            getAccountingPeriod(subs),
            getUpeAddressDetails(subs),
            getPrimaryContactDetails(subs),
            getSecondaryContactDetails(subs),
            getFilingMemberDetails(fmSafeId, fm)
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

  def retrieveSubscriptionInformation(id: String, plrReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    subscriptionConnectors
      .getSubscriptionInformation(plrReference)
      .flatMap { httpResponse =>
        httpResponse.status match {
          case status if Set(NOT_FOUND, BAD_REQUEST, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).contains(status) =>
            Future.successful(handleErrorStatus(status))
          case _ =>
            val jsonBody         = httpResponse.json
            val request          = jsonBody.as[SubscriptionResponse]
            val subscriptionData = extractSubscriptionData(request.success)
            val jsonData         = Json.toJson(subscriptionData)

            repository.upsert(id, jsonData).map { _ =>
              logger.info(s"Upserted data for id: $id")
              httpResponse
            }
        }
      }
      .recover { case e: Exception =>
        logger.warn("Subscription Information Missing or other error", e)
        ReadsubscriptionError
      }

  private def handleErrorStatus(status: Int): HttpResponse =
    status match {
      case NOT_FOUND             => HttpResponse(NOT_FOUND, Json.obj("error" -> "Resource not found").toString())
      case BAD_REQUEST           => HttpResponse(BAD_REQUEST, Json.obj("error" -> "Bad request from EIS").toString())
      case UNPROCESSABLE_ENTITY  => HttpResponse(UNPROCESSABLE_ENTITY, Json.obj("error" -> "Unprocessable entity").toString())
      case INTERNAL_SERVER_ERROR => HttpResponse(INTERNAL_SERVER_ERROR, Json.obj("error" -> "Internal server error").toString())
      case SERVICE_UNAVAILABLE   => HttpResponse(SERVICE_UNAVAILABLE, Json.obj("error" -> "Service unavailable").toString())
      case _                     => ReadsubscriptionError
    }

  private def extractSubscriptionData(sub: SubscriptionSuccess): Option[Subscription] =
    try {
      // Directly access fields without wrapping them in Option, as they are not optional at the top level
      val upeDetails               = sub.upeDetails
      val accountingPeriod         = sub.accountingPeriod
      val primaryContactDetails    = sub.primaryContactDetails
      val secondaryContactDetails  = sub.secondaryContactDetails
      val upeCorrespAddressDetails = sub.upeCorrespAddressDetails

      Some(
        Subscription(
          domesticOrMne = if (upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk,
          groupDetailStatus = RowStatus.Completed,
          accountingPeriod = Some(
            uk.gov.hmrc.pillar2.models.AccountingPeriod(
              startDate = accountingPeriod.startDate,
              endDate = accountingPeriod.endDate,
              duetDate = accountingPeriod.duetDate
            )
          ),
          primaryContactName = Some(primaryContactDetails.name),
          primaryContactEmail = Some(primaryContactDetails.emailAddress),
          primaryContactTelephone = primaryContactDetails.telephone,
          secondaryContactName = Some(secondaryContactDetails.name),
          secondaryContactEmail = Some(secondaryContactDetails.emailAddress),
          secondaryContactTelephone = secondaryContactDetails.telephone,
          correspondenceAddress = Some(
            SubscriptionAddress(
              addressLine1 = upeCorrespAddressDetails.addressLine1,
              addressLine2 = upeCorrespAddressDetails.addressLine2.orElse(Some("")),
              addressLine3 = upeCorrespAddressDetails.addressLine3.orElse(Some("")).getOrElse(""),
              addressLine4 = None, // Assuming this field is always None in your current logic
              postalCode = upeCorrespAddressDetails.postCode.orElse(Some("")),
              countryCode = upeCorrespAddressDetails.countryCode
            )
          ),
          accountStatus = Some(uk.gov.hmrc.pillar2.models.AccountStatus(sub.accountStatus.inactive))
        )
      )
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to extract subscription data: ${e.getMessage}", e)
        logger.warn(s"SubscriptionSuccess input: $sub")
        None
    }

  //  private def extractSubscriptionData(sub: SubscriptionSuccess): Option[Subscription] =
//    try {
//      val upeDetailsOpt               = Option(sub.upeDetails)
//      val accountingPeriodOpt         = Option(sub.accountingPeriod)
//      val primaryContactDetailsOpt    = Option(sub.primaryContactDetails)
//      val secondaryContactDetailsOpt  = Option(sub.secondaryContactDetails)
//      val upeCorrespAddressDetailsOpt = Option(sub.upeCorrespAddressDetails)
//
//      for {
//        upeDetails               <- upeDetailsOpt
//        accountingPeriod         <- accountingPeriodOpt
//        primaryContactDetails    <- primaryContactDetailsOpt
//        secondaryContactDetails  <- secondaryContactDetailsOpt
//        upeCorrespAddressDetails <- upeCorrespAddressDetailsOpt
//      } yield {
//        logger.info(s"Constructing Subscription from SubscriptionSuccess: $sub")
//
//        val domesticOrMneValue = if (upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk
//        logger.info(s"Constructed domesticOrMne: $domesticOrMneValue")
//
//        val accountStatusValue = Some(uk.gov.hmrc.pillar2.models.AccountStatus(sub.accountStatus.inactive))
//        logger.info(s"Constructed accountStatus: $accountStatusValue")
//
//        val correspondenceAddressValue = Some(
//          SubscriptionAddress(
//            addressLine1 = upeCorrespAddressDetails.addressLine1,
//            addressLine2 = upeCorrespAddressDetails.addressLine2,
//            addressLine3 = upeCorrespAddressDetails.addressLine3.getOrElse(""),
//            addressLine4 = None,
//            postalCode = upeCorrespAddressDetails.postCode,
//            countryCode = upeCorrespAddressDetails.countryCode
//          )
//        )
//        logger.info(s"Constructed correspondenceAddress: $correspondenceAddressValue")
//
//        Subscription(
//          domesticOrMne = domesticOrMneValue,
//          groupDetailStatus = RowStatus.Completed,
//          accountingPeriod = Some(
//            uk.gov.hmrc.pillar2.models.AccountingPeriod(
//              startDate = accountingPeriod.startDate,
//              endDate = accountingPeriod.endDate,
//              duetDate = accountingPeriod.duetDate
//            )
//          ),
//          primaryContactName = Some(primaryContactDetails.name),
//          primaryContactEmail = Some(primaryContactDetails.emailAddress),
//          primaryContactTelephone = primaryContactDetails.telephone,
//          secondaryContactName = Some(secondaryContactDetails.name),
//          secondaryContactEmail = Some(secondaryContactDetails.emailAddress),
//          secondaryContactTelephone = secondaryContactDetails.telephone,
//          correspondenceAddress = correspondenceAddressValue,
//          accountStatus = accountStatusValue
//        )
//      }
//    } catch {
//      case e: Exception =>
//        logger.warn(s"Failed to extract subscription data: ${e.getMessage}", e)
//        logger.warn(s"SubscriptionSuccess input: $sub")
//        None
//    }

  //  private def extractSubscriptionData(sub: SubscriptionSuccess): Option[Subscription] =
//    try {
//      val upeDetailsOpt               = Option(sub.upeDetails)
//      val accountingPeriodOpt         = Option(sub.accountingPeriod)
//      val primaryContactDetailsOpt    = Option(sub.primaryContactDetails)
//      val secondaryContactDetailsOpt  = Option(sub.secondaryContactDetails)
//      val upeCorrespAddressDetailsOpt = Option(sub.upeCorrespAddressDetails)
//
//      for {
//        upeDetails               <- upeDetailsOpt
//        accountingPeriod         <- accountingPeriodOpt
//        primaryContactDetails    <- primaryContactDetailsOpt
//        secondaryContactDetails  <- secondaryContactDetailsOpt
//        upeCorrespAddressDetails <- upeCorrespAddressDetailsOpt
//      } yield Subscription(
//        domesticOrMne = if (upeDetails.domesticOnly) MneOrDomestic.UkAndOther else MneOrDomestic.Uk,
//        groupDetailStatus = RowStatus.Completed,
//        accountingPeriod = Some(
//          uk.gov.hmrc.pillar2.models.AccountingPeriod(
//            startDate = accountingPeriod.startDate,
//            endDate = accountingPeriod.endDate,
//            duetDate = accountingPeriod.duetDate
//          )
//        ),
//        primaryContactName = Some(primaryContactDetails.name),
//        primaryContactEmail = Some(primaryContactDetails.emailAddress),
//        primaryContactTelephone = primaryContactDetails.telephone,
//        secondaryContactName = Some(secondaryContactDetails.name),
//        secondaryContactEmail = Some(secondaryContactDetails.emailAddress),
//        secondaryContactTelephone = secondaryContactDetails.telephone,
//        correspondenceAddress = Some(
//          SubscriptionAddress(
//            addressLine1 = upeCorrespAddressDetails.addressLine1,
//            addressLine2 = upeCorrespAddressDetails.addressLine2,
//            addressLine3 = upeCorrespAddressDetails.addressLine3.getOrElse(""),
//            addressLine4 = None, // Assuming this field is always None in your current logic
//            postalCode = upeCorrespAddressDetails.postCode,
//            countryCode = upeCorrespAddressDetails.countryCode
//          )
//        ),
//        accountStatus = Some(uk.gov.hmrc.pillar2.models.AccountStatus(sub.accountStatus.inactive))
//      )
//    } catch {
//      case e: Exception =>
//        logger.warn(s"Failed to extract subscription data: ${e.getMessage}")
//        None
//    }

  private val ReadsubscriptionError = HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription")
  private val subscriptionError     = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in Subscription"))

  private def getUpeDetails(upeSafeId: String, registration: Registration, fm: FilingMember, subscription: Subscription): UpeDetails = {
    val domesticOnly   = if (subscription.domesticOrMne == MneOrDomestic.uk) true else false
    val isFilingMember = fm.nfmConfirmation
    registration.isUPERegisteredInUK match {
      case true =>
        val withIdData = registration.withIdRegData.getOrElse(throw new Exception("Malformed Registration data"))
        registration.orgType match {
          case Some(EntityType.UKLimitedCompany) =>
            val incorporatedEntityRegistrationData =
              withIdData.incorporatedEntityRegistrationData.getOrElse(throw new Exception("Malformed Register WithIddata"))
            val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
            val name = incorporatedEntityRegistrationData.companyProfile.companyName
            val utr  = incorporatedEntityRegistrationData.ctutr

            UpeDetails(Some(upeSafeId), Some(crn), Some(utr), name, LocalDate.now(), domesticOnly, isFilingMember)

          case Some(EntityType.LimitedLiabilityPartnership) =>
            val partnershipEntityRegistrationData = withIdData.partnershipEntityRegistrationData.getOrElse(throw new Exception("Malformed LLP data"))
            val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
            val crn            = companyProfile.companyNumber
            val name           = companyProfile.companyName
            val utr            = partnershipEntityRegistrationData.sautr

            UpeDetails(Some(upeSafeId), Some(crn), utr, name, LocalDate.now(), domesticOnly, isFilingMember)

          case _ => throw new Exception("Invalid Org Type")
        }
      case false =>
        val withoutId = registration.withoutIdRegData.getOrElse(throw new Exception("Malformed without id data"))
        val upeName   = withoutId.upeNameRegistration

        UpeDetails(Some(upeSafeId), None, None, upeName, LocalDate.now(), domesticOnly, isFilingMember)
    }
  }

  private def getFilingMemberDetails(filingMemberSafeId: Option[String], fm: FilingMember): Option[FilingMemberDetails] =
    filingMemberSafeId match {
      case Some(fmSafeId) =>
        fm.nfmConfirmation match {
          case true =>
            fm.isNfmRegisteredInUK match {
              case Some(true) =>
                val withIdData = fm.withIdRegData.getOrElse(throw new Exception("Malformed Grs Response data"))
                fm.orgType match {
                  case Some(EntityType.UKLimitedCompany) =>
                    val incorporatedEntityRegistrationData =
                      withIdData.incorporatedEntityRegistrationData.getOrElse(throw new Exception("Malformed RegisterWithid data"))
                    val crn  = incorporatedEntityRegistrationData.companyProfile.companyNumber
                    val name = incorporatedEntityRegistrationData.companyProfile.companyName
                    val utr  = incorporatedEntityRegistrationData.ctutr

                    Some(FilingMemberDetails(fmSafeId, Some(crn), Some(utr), name))
                  case Some(EntityType.LimitedLiabilityPartnership) =>
                    val partnershipEntityRegistrationData =
                      withIdData.partnershipEntityRegistrationData.getOrElse(throw new Exception("Malformed LLP data"))
                    val companyProfile = partnershipEntityRegistrationData.companyProfile.getOrElse(throw new Exception("Malformed company Profile"))
                    val crn            = companyProfile.companyNumber
                    val name           = companyProfile.companyName
                    val utr            = partnershipEntityRegistrationData.sautr
                    Some(FilingMemberDetails(fmSafeId, Some(crn), utr, name))

                  case _ => throw new Exception("Filing Member: Invalid Org Type")
                }
              case Some(false) =>
                val upeName = fm.withoutIdRegData.fold("")(withoutId => withoutId.registeredFmName)
                Some(FilingMemberDetails(fmSafeId, None, None, upeName))
              case _ => throw new Exception("Filing Member: Invalid Uk or other resident")
            }

          case false => None
        }
      case _ => None
    }

  private def getUpeAddressDetails(subscription: Subscription): UpeCorrespAddressDetails = {
    val subsAddress = subscription.correspondenceAddress.getOrElse(throw new Exception("Malformed Subscription Address"))
    UpeCorrespAddressDetails(
      addressLine1 = subsAddress.addressLine1,
      addressLine2 = subsAddress.addressLine2,
      addressLine3 = Some(subsAddress.addressLine3),
      addressLine4 = subsAddress.addressLine4,
      postCode = subsAddress.postalCode,
      countryCode = subsAddress.countryCode
    )
  }

  private def getAccountingPeriod(subscription: Subscription): AccountingPeriod = {
    val accountingPeriod = subscription.accountingPeriod.getOrElse(throw new Exception("Malformed accountingPeriod data"))
    AccountingPeriod(accountingPeriod.startDate, accountingPeriod.endDate)
  }

  private def getPrimaryContactDetails(subscription: Subscription): ContactDetailsType =
    ContactDetailsType(
      name = subscription.primaryContactName.fold("")(primary => primary),
      telephone = subscription.primaryContactTelephone,
      emailAddress = subscription.primaryContactEmail.fold("")(email => email)
    )

  private def getSecondaryContactDetails(subscription: Subscription): Option[ContactDetailsType] =
    subscription.secondaryContactName.fold(Option.empty[ContactDetailsType])(secondaryName =>
      Some(
        ContactDetailsType(
          name = secondaryName,
          telephone = subscription.secondaryContactTelephone,
          emailAddress = subscription.secondaryContactEmail.fold("")(email => email)
        )
      )
    )
}
