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
import play.api.Logger
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.financial.FinancialDataResponse
import uk.gov.hmrc.pillar2.models.{FinancialDataError, FinancialDataErrorResponses}

import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject() (val config: AppConfig, val http: HttpClient) {
  implicit val logger: Logger = Logger(this.getClass.getName)

  def retrieveFinancialData(idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FinancialDataResponse] = {
    val serviceName = "financial-data"
    http
      .GET[HttpResponse](
        url = s"${config.baseUrl(serviceName)}/ZPLR/$idNumber/PLR",
        headers = extraHeaders(config, serviceName)
      )(httpReads, hc, ec)
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[FinancialDataResponse])
          case _ =>
            response.json.asOpt[FinancialDataError] match {
              case Some(error) => Future.failed(new RuntimeException(s"Financial data error: ${error.code}, ${error.reason}"))
              case None =>
                response.json.asOpt[FinancialDataErrorResponses] match {
                  case Some(errorResponse) =>
                    Future.failed(new RuntimeException(s"Multiple financial data errors: ${errorResponse.failures.map(_.reason).mkString(", ")}"))
                  case None => Future.failed(new RuntimeException("Unexpected error response format"))
                }
            }
        }
      }
  }
}
