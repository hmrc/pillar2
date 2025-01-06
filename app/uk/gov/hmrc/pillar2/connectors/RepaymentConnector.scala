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
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail

import scala.concurrent.{ExecutionContext, Future}

class RepaymentConnector @Inject() (implicit
  ec:         ExecutionContext,
  val config: AppConfig,
  val http:   HttpClientV2
) extends Logging {

  def sendRepaymentDetails(
    repaymentRequest: RepaymentRequestDetail
  )(implicit hc:      HeaderCarrier): Future[HttpResponse] = {
    val serviceName = "create-repayment"
    val url         = s"${config.baseUrl(serviceName)}"
    implicit val writes: Writes[RepaymentRequestDetail] = RepaymentRequestDetail.format
    http
      .post(url"$url")
      .setHeader(extraHeaders(config, serviceName): _*)
      .withBody(Json.toJson(repaymentRequest))
      .execute[HttpResponse]
  }
}
