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

package uk.gov.hmrc.pillar2.models.grs

import play.api.libs.json.*

sealed trait VerificationStatus

object VerificationStatus {
  case object Pass extends VerificationStatus
  case object Fail extends VerificationStatus
  case object Unchallenged extends VerificationStatus
  case object CtEnrolled extends VerificationStatus
  case object SaEnrolled extends VerificationStatus

  given format: Format[VerificationStatus] = new Format[VerificationStatus] {
    override def reads(json: JsValue): JsResult[VerificationStatus] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "PASS"         => JsSuccess(Pass)
          case "FAIL"         => JsSuccess(Fail)
          case "UNCHALLENGED" => JsSuccess(Unchallenged)
          case "CT_ENROLLED"  => JsSuccess(CtEnrolled)
          case "SA_ENROLLED"  => JsSuccess(SaEnrolled)
          case s              => JsError(s"$s is not a valid VerificationStatus")
        }
      case e: JsError => e
    }

    override def writes(o: VerificationStatus): JsValue = o match {
      case Pass         => JsString("PASS")
      case Fail         => JsString("FAIL")
      case Unchallenged => JsString("UNCHALLENGED")
      case CtEnrolled   => JsString("CT_ENROLLED")
      case SaEnrolled   => JsString("SA_ENROLLED")
    }
  }
}

final case class BusinessVerificationResult(
  verificationStatus: VerificationStatus
)

object BusinessVerificationResult {
  given format: OFormat[BusinessVerificationResult] =
    Json.format[BusinessVerificationResult]
}
