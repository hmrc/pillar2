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

import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.hip.{ApiSuccess, ApiSuccessResponse}

import java.time.ZonedDateTime

trait UKTaxReturnDataFixture {

  implicit val pillar2Id: String = "XMPLR0000000012"
  val successResponse: ApiSuccessResponse = ApiSuccessResponse(
    ApiSuccess(
      processingDate = ZonedDateTime.parse("2024-03-14T09:26:17Z"),
      formBundleNumber = "123456789012345",
      chargeReference = "12345678"
    )
  )
  val httpCreated: HttpResponse = HttpResponse(CREATED, Json.toJson(successResponse).toString())

}
