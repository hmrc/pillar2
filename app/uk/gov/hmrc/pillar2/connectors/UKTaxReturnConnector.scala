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

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.connectors.headers.UKTaxReturnHeaders
import uk.gov.hmrc.pillar2.models.uktrsubmissions.UktrSubmission

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class UKTaxReturnConnector @Inject() (
  val http:    HttpClient,
  val config:  AppConfig
)(implicit ec: ExecutionContext)
    extends UKTaxReturnHeaders {

  def submitUKTaxReturn(
    payload:     UktrSubmission,
    pillar2Id:   String
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val serviceName = "submit-uk-tax-return"
    val url         = s"${config.baseUrl(serviceName)}"

    http
      .POST[UktrSubmission, HttpResponse](
        url,
        payload,
        generateHeaders(pillar2Id)
      )
  }

}
