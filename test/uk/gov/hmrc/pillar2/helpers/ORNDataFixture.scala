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

package uk.gov.hmrc.pillar2.helpers

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.pillar2.models.orn.{GetORNSuccess, GetORNSuccessResponse, ORNRequest}

import java.time.{LocalDate, ZonedDateTime}

trait ORNDataFixture {

  val fromDate: LocalDate = LocalDate.now()
  val toDate:   LocalDate = fromDate.plusYears(1)

  val downstreamUrl = "/RESTAdapter/plr/overseas-return-notification"

  val ornRequest: ORNRequest =
    ORNRequest(
      accountingPeriodFrom = LocalDate.now(),
      accountingPeriodTo = LocalDate.now().plusYears(1),
      filedDateGIR = LocalDate.now().minusDays(10),
      countryGIR = "US",
      reportingEntityName = "Newco PLC",
      TIN = "US12345678",
      issuingCountryTIN = "US"
    )

  val ornRequestJson: JsValue = Json.toJson(ornRequest)

  val ornResponse: GetORNSuccessResponse = GetORNSuccessResponse(
    GetORNSuccess(
      processingDate = ZonedDateTime.now(),
      ornRequest.accountingPeriodFrom,
      ornRequest.accountingPeriodTo,
      ornRequest.filedDateGIR,
      ornRequest.countryGIR,
      ornRequest.reportingEntityName,
      ornRequest.TIN,
      ornRequest.issuingCountryTIN
    )
  )

  val ornSubmitResponse: JsObject = Json.obj(
    "success" -> Json.obj(
      "processingDate"   -> "2024-03-14T09:26:17Z",
      "formBundleNumber" -> "123456789012345"
    )
  )
}
