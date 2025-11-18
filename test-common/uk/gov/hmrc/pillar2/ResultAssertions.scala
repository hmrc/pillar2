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

package uk.gov.hmrc.pillar2

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.api.mvc.Result
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{contentAsJson, contentAsString}

import scala.concurrent.Future

trait ResultAssertions { me: Matchers with Inside with DefaultAwaitTimeout =>

  def assertJsonBodyOf[T](result: Future[Result])(block: T => Assertion)(using reads: Reads[T], mat: Materializer): Assertion =
    inside(contentAsJson(result).validate[T]) {
      case JsSuccess(model, _) => block(model)
      case JsError(errs) =>
        fail(s"${contentAsString(result)} \n\nFailed JSON validation. Missing fields: \n ${JsError.toJson(errs)}\n")
    }

  def assertErrorResponse(result: Future[Result], code: String, message: String)(using mat: Materializer): Assertion = {
    val errorResponse = contentAsJson(result)
    (errorResponse \ "code").as[String]    shouldBe code
    (errorResponse \ "message").as[String] shouldBe message
  }
}
