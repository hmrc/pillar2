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
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hods.subscription.request.CreateSubscriptionRequest

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject() (
  val config: AppConfig,
  val http:   HttpClient
) {

  def sendCreateSubscriptionInformation(
    suscription: CreateSubscriptionRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription"
    http.POST[CreateSubscriptionRequest, HttpResponse](
      config.baseUrl(serviceName),
      suscription,
      headers = extraHeaders(config, serviceName)
    )(wts = CreateSubscriptionRequest.format, rds = httpReads, hc = hc, ec = ec)
  }

  def getSubscriptionInformation(plrReference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-subscription"
    val url         = s"${config.baseUrl(serviceName)}/$plrReference"
    http
      .GET[HttpResponse](url, headers = extraHeaders(config, serviceName))(httpReads, hc, ec)
      .map { response =>
        println(s"Response received: ${response.toString}")
        response
      }
      .recover { case ex: Throwable =>
        println(s"Error while fetching subscription information: ${ex.getMessage}")
        throw ex
      }
  }

}
