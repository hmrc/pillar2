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
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.errors.ObligationsAndSubmissionsError
import uk.gov.hmrc.pillar2.models.obligationsAndSubmissions.ObligationsAndSubmissionsResponse

import scala.concurrent.{ExecutionContext, Future}

class ObligationsAndSubmissionsConnector @Inject() (val config: AppConfig, val httpClient: HttpClientV2) {

  def getObligationsAndSubmissions(plrReference: String)(implicit
    hc:                                          HeaderCarrier,
    ec:                                          ExecutionContext
  ): Future[ObligationsAndSubmissionsResponse] = {
    val serviceName = "obligations-and-submissions"
    val url =
      s"${config.baseUrl(serviceName)}/$plrReference" //TODO: Add query params in url (e.g ?01-01-2023&?01-31-12-2024) for start and end date? Pass plr through implicitly?
    httpClient
      .get(url"$url")
      .setHeader(extraHeaders(config, serviceName): _*)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future successful response.json.as[ObligationsAndSubmissionsResponse]
          case _ =>
            Future.failed(ObligationsAndSubmissionsError)
        }
      }
  }
}
