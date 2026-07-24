/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hods.subscription.common.EtmpAmendSubscriptionRequest
import uk.gov.hmrc.pillar2.models.hods.subscription.requests.SubscriptionDataCreate

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject() (val config: AppConfig, val http: HttpClientV2) {

  given logger: Logger = Logger(this.getClass.getName)

  def sendCreateSubscriptionInformation(
    subscription: SubscriptionDataCreate
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    logger.info(s"SubscriptionConnector - CreateSubscriptionRequest going to Etmp")
    val serviceName = "create-subscription"
    val createSubscriptionUrl: URL = url"${config.baseUrl(serviceName)}"
    http
      .post(createSubscriptionUrl)
      .setHeader(extraHeaders(config, serviceName)*)
      .withBody(Json.toJson(subscription))
      .execute[HttpResponse]
  }

  def getSubscriptionInformation(
    plrReference: String
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription-v2"
    val getSubscriptionUrl: URL = url"${config.baseUrl(serviceName)}/$plrReference"
    http
      .get(getSubscriptionUrl)
      .setHeader(extraHeaders(config, serviceName)*)
      .execute[HttpResponse]
  }

  def amendSubscriptionInformation(
    amendRequest: EtmpAmendSubscriptionRequest
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName          = "amend-subscription-v2"
    val amendSubscriptionUrl = url"${config.baseUrl(serviceName)}"
    given writes: Writes[EtmpAmendSubscriptionRequest] = EtmpAmendSubscriptionRequest.format
    http
      .put(amendSubscriptionUrl)
      .withBody(Json.toJson(amendRequest))
      .setHeader(extraHeaders(config, serviceName)*)
      .execute[HttpResponse]
  }
}
