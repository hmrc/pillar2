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

package uk.gov.hmrc.pillar2.models.hods.subscription.request

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pillar2.models.AccountingPeriod
import uk.gov.hmrc.pillar2.models.hods.subscription.common._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

case class RequestDetail(
  upeDetails:               UpeDetails,
  accountingPeriod:         AccountingPeriod,
  upeCorrespAddressDetails: UpeCorrespAddressDetails,
  primaryContactDetails:    ContactDetailsType,
  secondaryContactDetails:  Option[ContactDetailsType],
  filingMemberDetails:      Option[FilingMemberDetails]
)

object RequestDetail {
  implicit val requestDetailFormats: OFormat[RequestDetail] =
    Json.format[RequestDetail]
}

case class RequestParameters(paramName: String, paramValue: String)

object RequestParameters {
  implicit val formats: OFormat[RequestParameters] =
    Json.format[RequestParameters]
}

case class RequestCommonForSubscription(
  regime:                   String,
  conversationID:           Option[String] = None,
  receiptDate:              String,
  acknowledgementReference: String,
  originatingSystem:        String,
  requestParameters:        Option[Seq[RequestParameters]]
)

object RequestCommonForSubscription {
  implicit val requestCommonForSubscriptionFormats: OFormat[RequestCommonForSubscription] =
    Json.format[RequestCommonForSubscription]

  private val mdtp = "MDTP"

  def createRequestCommonForSubscription(): RequestCommonForSubscription = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    //Generate a 32 chars UUID without hyphens
    val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")

    val v = RequestCommonForSubscription(
      regime = "PIL2",
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = acknowledgementReference,
      originatingSystem = mdtp,
      requestParameters = None
    )
    println(s"what is v 000000)))))))))))))- $v")
    v
  }

}

case class SubscriptionRequest(
  requestCommon: RequestCommonForSubscription,
  requestDetail: RequestDetail
)

object SubscriptionRequest {
  implicit val format: OFormat[SubscriptionRequest] =
    Json.format[SubscriptionRequest]
}
