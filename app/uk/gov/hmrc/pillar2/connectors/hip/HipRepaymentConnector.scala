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

package uk.gov.hmrc.pillar2.connectors.hip

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.pillar2.connectors.hipHeadersNew
//import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.connectors.RepaymentConnector
import uk.gov.hmrc.pillar2.models.errors.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.hods.repayment.request.RepaymentRequestDetail

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class HipRepaymentConnector @Inject() (
  val ec:     ExecutionContext,
  val config: AppConfig,
  val http:   HttpClientV2
) extends RepaymentConnector {

  val serviceName   = "hip.create-repayment"
  val correlationId = UUID.randomUUID().toString

  override def sendRepaymentDetails(
    repaymentRequest: RepaymentRequestDetail
  )(using
    hc: HeaderCarrier
  ): Future[HttpResponse] =

    val correlationId = UUID.randomUUID().toString

    http
      .post(url"$url")
      .setHeader(hipHeadersNew(correlationId, config)*)
      .withBody(Json.toJson(repaymentRequest))
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }

}
