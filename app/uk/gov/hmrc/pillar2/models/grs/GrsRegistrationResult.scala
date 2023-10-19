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

import play.api.libs.json._

sealed trait RegistrationStatus

object RegistrationStatus {
  case object Registered extends RegistrationStatus
  case object RegistrationFailed extends RegistrationStatus
  case object RegistrationNotCalled extends RegistrationStatus

  implicit val format: Format[RegistrationStatus] = new Format[RegistrationStatus] {
    override def reads(json: JsValue): JsResult[RegistrationStatus] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "REGISTERED"              => JsSuccess(Registered)
          case "REGISTRATION_FAILED"     => JsSuccess(RegistrationFailed)
          case "REGISTRATION_NOT_CALLED" => JsSuccess(RegistrationNotCalled)
          case s                         => JsError(s"$s is not a valid RegistrationStatus")
        }
      case e: JsError => e
    }

    override def writes(o: RegistrationStatus): JsValue = o match {
      case Registered            => JsString("REGISTERED")
      case RegistrationFailed    => JsString("REGISTRATION_FAILED")
      case RegistrationNotCalled => JsString("REGISTRATION_NOT_CALLED")
    }
  }
}

final case class GrsRegistrationResult(
  registrationStatus:          RegistrationStatus,
  registeredBusinessPartnerId: Option[String],
  failures:                    Option[Seq[GrsRegistrationResultFailure]]
)

object GrsRegistrationResult {
  implicit val format: OFormat[GrsRegistrationResult] =
    Json.format[GrsRegistrationResult]
}

final case class GrsRegistrationResultFailure(
  code:   String,
  reason: String
)

object GrsRegistrationResultFailure {
  implicit val format: OFormat[GrsRegistrationResultFailure] =
    Json.format[GrsRegistrationResultFailure]
}
