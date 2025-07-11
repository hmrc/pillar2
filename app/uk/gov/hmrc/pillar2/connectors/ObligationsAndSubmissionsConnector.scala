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

import com.google.inject.Inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.UnexpectedResponse

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ObligationsAndSubmissionsConnector @Inject() (val config: AppConfig, val httpClient: HttpClientV2) {

  def getObligationsAndSubmissions(fromDate: LocalDate, toDate: LocalDate)(implicit
    hc:                                      HeaderCarrier,
    ec:                                      ExecutionContext,
    pillar2Id:                               String
  ): Future[HttpResponse] = {
    val serviceName = "obligations-and-submissions"
    val url =
      s"${config.baseUrl(serviceName)}?fromDate=${fromDate.toString}&toDate=${toDate.toString}"
    httpClient
      .get(url"$url")
      .setHeader(hipHeaders(config = config): _*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
  }
}
