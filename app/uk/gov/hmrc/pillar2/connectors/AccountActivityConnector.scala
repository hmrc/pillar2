/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.http.given
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.UnexpectedResponse
import uk.gov.hmrc.pillar2.models.accountactivity.AccountActivityRequest

import scala.concurrent.{ExecutionContext, Future}

class AccountActivityConnector @Inject() (val config: AppConfig, val httpClient: HttpClientV2) {
  private val accountActivityBaseUrl = url"${config.baseUrl("account-activity")}"

  def retrieveAccountActivity(request: AccountActivityRequest, pillarId: String)(using HeaderCarrier, ExecutionContext): Future[HttpResponse] =
    httpClient
      .get(accountActivityBaseUrl)
      .transform(
        _.withQueryStringParameters(
          "fromDate" -> request.fromDate.toString,
          "toDate"   -> request.toDate.toString
        )
      )
      .setHeader(("X-Message-Type", "ACCOUNT_ACTIVITY") +: hipHeaders(config)(using summon[HeaderCarrier], pillarId)*)
      .execute[HttpResponse]
      .recoverWith { case _ => Future.failed(UnexpectedResponse) }
}
