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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.models.financial.FinancialDataResponse
import uk.gov.hmrc.pillar2.models.{FinancialDataError, FinancialDataErrorResponses}

import java.net.URLEncoder
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject() (val config: AppConfig, val httpClient: HttpClientV2) {
  implicit val logger: Logger = Logger(this.getClass.getName)
  Seq("Content-Type" -> "application/json")

  def retrieveFinancialData(idNumber: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit
    hc:                               HeaderCarrier,
    ec:                               ExecutionContext
  ): Future[FinancialDataResponse] = {
    val serviceName = "financial-data"
    val queryParams = Seq(
      "dateFrom"                   -> dateFrom.toString,
      "dateTo"                     -> dateTo.toString,
      "onlyOpenItems"              -> "false",
      "includeLocks"               -> "false",
      "calculateAccruedInterest"   -> "true",
      "customerPaymentInformation" -> "true"
    )
    def encode(params: Seq[(String, String)]): String =
      params
        .map { case (key, value) =>
          s"${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        .mkString("&")
    val queryString = encode(queryParams)
    httpClient
      .get(url"${config.baseUrl(serviceName)}/ZPLR/$idNumber/PLR$queryString")
      .setHeader(extraHeaders(config, serviceName): _*)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future successful response.json.as[FinancialDataResponse]
          case _  => Future failed response.json.asOpt[FinancialDataError].getOrElse(response.json.as[FinancialDataErrorResponses])
        }
      }
  }
}
