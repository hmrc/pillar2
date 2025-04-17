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

package uk.gov.hmrc.pillar2.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.orn.ORNRequest

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ORNConnector @Inject() (val config: AppConfig, val http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {
  val serviceName = "overseas-return-notification"
  val url: String = config.baseUrl(serviceName)

  def submitOrn(ornRequest: ORNRequest)(implicit hc: HeaderCarrier, pillar2Id: String): Future[HttpResponse] = {
    logger.info(s"Calling $url to submit a ORN")
    http
      .post(url"$url")
      .withBody(Json.toJson(ornRequest))
      .setHeader(hipHeaders(config = config, serviceName = serviceName): _*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
  }

  def amendOrn(ornRequest: ORNRequest)(implicit hc: HeaderCarrier, pillar2Id: String): Future[HttpResponse] = {
    logger.info(s"Calling $url to amend a ORN")
    http
      .put(url"$url")
      .withBody(Json.toJson(ornRequest))
      .setHeader(hipHeaders(config = config, serviceName = serviceName): _*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
  }

  def getOrn(fromDate: LocalDate, toDate: LocalDate)(implicit
    hc:                HeaderCarrier,
    ec:                ExecutionContext,
    pillar2Id:         String
  ): Future[HttpResponse] = {
    val serviceName = "overseas-return-notification"
    val url =
      s"${config.baseUrl(serviceName)}?accountingPeriodFrom=${fromDate.toString}&accountingPeriodTo=${toDate.toString}"
    http
      .get(url"$url")
      .setHeader(hipHeaders(config = config, serviceName = serviceName): _*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
  }
}
