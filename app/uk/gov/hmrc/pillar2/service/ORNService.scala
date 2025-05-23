/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.connectors.ORNConnector
import uk.gov.hmrc.pillar2.models.orn.{GetORNSuccessResponse, ORNRequest, ORNSuccessResponse}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ORNService @Inject() (
  ornConnector: ORNConnector
)(implicit ec:  ExecutionContext) {

  def submitOrn(ornRequest: ORNRequest)(implicit hc: HeaderCarrier, pillar2Id: String): Future[ORNSuccessResponse] =
    ornConnector
      .submitOrn(ornRequest)
      .flatMap(convertToORNApiResult)

  def amendOrn(ornRequest: ORNRequest)(implicit hc: HeaderCarrier, pillar2Id: String): Future[ORNSuccessResponse] =
    ornConnector
      .amendOrn(ornRequest)
      .flatMap(convertToORNApiResult)

  def getOrn(
    fromDate:    LocalDate,
    toDate:      LocalDate
  )(implicit hc: HeaderCarrier, pillar2Id: String): Future[GetORNSuccessResponse] =
    ornConnector
      .getOrn(fromDate, toDate)
      .flatMap(convertToGetORNApiResult)
}
