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
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hods.subscription.common.ETMPAmendSubscriptionSuccess
import uk.gov.hmrc.pillar2.models.hods.subscription.request.RequestDetail

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject() (
  val config: AppConfig,
  val http:   HttpClient
) {
  implicit val logger: Logger = Logger(this.getClass.getName)
  def sendCreateSubscriptionInformation(
    subscription: RequestDetail
  )(implicit hc:  HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription"
    logger.info(
      s"SubscriptionConnector - CreateSubscriptionRequest going to Etmp - ${Json.toJson(subscription)}"
    )
    http.POST[RequestDetail, HttpResponse](
      config.baseUrl(serviceName),
      subscription,
      headers = extraHeaders(config, serviceName)
    )(wts = RequestDetail.format, rds = httpReads, hc = hc, ec = ec)
  }

  def getSubscriptionInformation(plrReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription"
    val url         = s"${config.baseUrl(serviceName)}/$plrReference"
    http
      .GET[HttpResponse](url, headers = extraHeaders(config, serviceName))(httpReads, hc, ec)
  }

  def amendSubscriptionInformation(
    amendRequest: ETMPAmendSubscriptionSuccess
  )(implicit hc:  HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription"
    val url         = s"${config.baseUrl(serviceName)}"
    implicit val writes: Writes[ETMPAmendSubscriptionSuccess] = ETMPAmendSubscriptionSuccess.format
    http.PUT[ETMPAmendSubscriptionSuccess, HttpResponse](
      url,
      amendRequest,
      extraHeaders(config, serviceName)
    )(writes, httpReads, hc, ec)
  }

}
