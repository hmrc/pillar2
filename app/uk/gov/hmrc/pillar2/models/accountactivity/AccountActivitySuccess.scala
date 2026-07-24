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

package uk.gov.hmrc.pillar2.models.accountactivity

import play.api.libs.functional.syntax.given
import play.api.libs.json.*

import java.time.ZonedDateTime

final case class AccountActivitySuccess(
  processingDate:     ZonedDateTime,
  transactionDetails: Option[Seq[AccountActivityTransaction]]
)

object AccountActivitySuccess {
  given Format[AccountActivitySuccess] = Format(
    (
      (__ \ "success" \ "processingDate").read[ZonedDateTime] and
        (__ \ "success" \ "transactionDetails").readNullable[Seq[AccountActivityTransaction]]
    )(AccountActivitySuccess.apply),
    (
      (__ \ "processingDate").write[ZonedDateTime] and
        (__ \ "transactionDetails").writeNullable[Seq[AccountActivityTransaction]]
    )(s => (s.processingDate, s.transactionDetails))
  )
}
