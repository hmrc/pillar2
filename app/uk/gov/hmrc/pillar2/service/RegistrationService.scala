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
import uk.gov.hmrc.pillar2.connectors.RegistrationConnector
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, RegisterWithoutId}
import uk.gov.hmrc.pillar2.models.identifiers.{FilingMemberId, RegistrationId}
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject() (repository: RegistrationCacheRepository, dataSubmissionConnectors: RegistrationConnector)(implicit
  ec:                                            ExecutionContext
) extends Logging {

  def sendNoIdUpeRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      registration <- userAnswers.get(RegistrationId)
      data         <- registration.withoutIdRegData
      emailAddress <- data.emailAddress
      address      <- data.upeRegisteredAddress
    } yield registerWithoutId(
      data.upeNameRegistration,
      Address.fromAddress(address),
      ContactDetails(None, None, None, Some(emailAddress))
    )
  }.getOrElse {
    logger.warn("Upe Registration Information Missing")
    registerWithoutIdError
  }

  def sendNoIdFmRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      fm           <- userAnswers.get(FilingMemberId)
      data         <- fm.withoutIdRegData
      emailAddress <- data.fmEmailAddress
      address      <- data.registeredFmAddress
    } yield registerWithoutId(
      data.registeredFmName,
      Address.fromFmAddress(address),
      ContactDetails(None, None, None, Some(emailAddress))
    )
  }.getOrElse {
    logger.warn("Filing Member Registration Information Missing")
    registerWithoutIdError
  }

  private def registerWithoutId(businessName: String, address: Address, contactDetails: ContactDetails)(implicit
    hc:                                       HeaderCarrier,
    ec:                                       ExecutionContext
  ): Future[HttpResponse] =
    dataSubmissionConnectors
      .sendWithoutIDInformation(RegisterWithoutId(businessName, address, contactDetails))(hc, ec)

  private val registerWithoutIdError = Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "Response not received in registration"))

}
