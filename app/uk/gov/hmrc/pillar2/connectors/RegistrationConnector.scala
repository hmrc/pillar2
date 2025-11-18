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
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.hods.RegisterWithoutIDRequest

import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnector @Inject() (
  val config: AppConfig,
  val http:   HttpClientV2
) {
  given logger: Logger = Logger(this.getClass.getName)
  def sendWithoutIDInformation(registration: RegisterWithoutIDRequest)(using hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "register-without-id"
    http
      .post(url"${config.baseUrl(serviceName)}")
      .setHeader(extraHeaders(config, serviceName)*)
      .withBody(Json.toJson(registration))
      .execute[HttpResponse]
  }
}
