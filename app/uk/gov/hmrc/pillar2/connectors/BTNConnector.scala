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
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.btn.BTNRequest

import scala.concurrent.{ExecutionContext, Future}

class BTNConnector @Inject() (val config: AppConfig, val http: HttpClientV2)(implicit ec: ExecutionContext) {

  def sendBtn(btnRequst: BTNRequest, plrReference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val serviceName = "below-threshold-notification"
    http
      .post(url"${config.baseUrl("below-threshold-notification")}")
      .withBody(Json.toJson(btnRequst))
      .setHeader(hipHeaders(pillar2Id = plrReference, config = config, serviceName = serviceName): _*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
  }
}
