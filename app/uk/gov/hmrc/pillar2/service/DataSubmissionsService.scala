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

import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.models.hods.{Address, ContactDetails, RegisterWithoutId}
import uk.gov.hmrc.pillar2.models.identifiers.RegistrationId
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import play.api.Logging
import uk.gov.hmrc.pillar2.connectors.DataSubmissionsConnector
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataSubmissionsService @Inject() (repository: RegistrationCacheRepository, dataSubmissionConnectors: DataSubmissionsConnector)(implicit
  ec:                                               ExecutionContext
) extends Logging {

  /*  def sendBusinessRegistration(userAnswers: UserAnswers): Future[HttpResponse] = {
    for {
      registration <- userAnswers.get(RegistrationId)
      data         <- registration.withoutIdRegData
      phoneNumber  <- data.telephoneNumber
      emailAddress <- data.emailAddress
      address      <- data.upeRegisteredAddress

    } yield {
      registerWithoutId(
        data.upeNameRegistration,
        Address.fromAddress(address),
        ContactDetails(Some(phoneNumber), None, None, Some(emailAddress))
      )
    }
  }.getOrElse {
    logger.warn("Registration Information Missing")
    registerWithoutIdError
  }*/

  def sendBusinessRegistration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
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
    logger.warn("Registration Information Missing")
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
