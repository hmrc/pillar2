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

package uk.gov.hmrc.pillar2.connectors

import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hip.uktrsubmissions.UKTRSubmission

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UKTaxReturnConnector @Inject() (
  val http:   HttpClientV2,
  val config: AppConfig
)(using ec:   ExecutionContext) {

  def submitUKTaxReturn(
    payload:  UKTRSubmission
  )(using hc: HeaderCarrier, pillar2Id: String): Future[HttpResponse] = {
    val serviceName = "submit-uk-tax-return"
    val url         = s"${config.baseUrl(serviceName)}"

    http
      .post(url"$url")
      .setHeader(hipHeaders(config = config)*)
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
  }

  def amendUKTaxReturn(
    payload:  UKTRSubmission
  )(using hc: HeaderCarrier, pillar2Id: String): Future[HttpResponse] = {
    val serviceName = "amend-uk-tax-return"
    val url         = s"${config.baseUrl(serviceName)}"

    http
      .put(url"$url")
      .setHeader(hipHeaders(config = config)*)
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
  }
}
