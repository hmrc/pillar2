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

package uk.gov.hmrc.pillar2.models.hods.subscription.common

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class ContactDetailsTypeSpec extends AnyFreeSpec with Matchers {

  "ContactDetailsType" - {
    "must deserialize from JSON with 'telephone'" in {
      val json = Json.obj(
        "name" -> "test name",
        "telephone" -> "123456",
        "emailAddress" -> "test@test.com"
      )
      val expected = ContactDetailsType("test name", Some("123456"), "test@test.com")
      json.validate[ContactDetailsType] mustEqual JsSuccess(expected)
    }

    "must deserialize from JSON with 'phone' when 'telephone' is missing" in {
      val json = Json.obj(
        "name" -> "test name",
        "phone" -> "123456",
        "emailAddress" -> "test@test.com"
      )
      val expected = ContactDetailsType("test name", Some("123456"), "test@test.com")
      json.validate[ContactDetailsType] mustEqual JsSuccess(expected)
    }

    "must deserialize from JSON with neither 'telephone' nor 'phone'" in {
      val json = Json.obj(
        "name" -> "test name",
        "emailAddress" -> "test@test.com"
      )
      val expected = ContactDetailsType("test name", None, "test@test.com")
      json.validate[ContactDetailsType] mustEqual JsSuccess(expected)
    }

    "must prioritize 'telephone' over 'phone' if both are present" in {
      val json = Json.obj(
        "name" -> "test name",
        "telephone" -> "111111",
        "phone" -> "222222",
        "emailAddress" -> "test@test.com"
      )
      val expected = ContactDetailsType("test name", Some("111111"), "test@test.com")
      json.validate[ContactDetailsType] mustEqual JsSuccess(expected)
    }

    "must serialize to JSON with 'telephone'" in {
      val model = ContactDetailsType("test name", Some("123456"), "test@test.com")
      val expected = Json.obj(
        "name" -> "test name",
        "telephone" -> "123456",
        "emailAddress" -> "test@test.com"
      )
      Json.toJson(model) mustEqual expected
    }
  }
}

