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

package uk.gov.hmrc.pillar2.models.queries

import play.api.libs.json.JsPath
import uk.gov.hmrc.pillar2.models.UserAnswers

import scala.util.{Success, Try}

// Trait to represent a query that has a path
sealed trait Query {
  def path: JsPath
}

// Trait to represent a gettable value with a query path
sealed trait Gettable[A] extends Query

// Trait to represent a settable value with a cleanup method
sealed trait Settable[A] extends Query {
  def cleanup(value: Option[A], userAnswers: UserAnswers): Try[UserAnswers] =
    Success(userAnswers)
}

object GettableFactory {
  def create[A](path: JsPath): Gettable[A] = new Gettable[A] {
    override val path: JsPath = Option(path).getOrElse(JsPath \ "default")
  }

  def createSettable[A](path: JsPath): GettableSettable[A] = new GettableSettable[A] {
    override val path: JsPath = Option(path).getOrElse(JsPath \ "default")
  }
}

// Trait that combines Gettable and Settable functionality
sealed trait GettableSettable[A] extends Gettable[A] with Settable[A]

// External interface for managing Gettable and Settable instances
trait ExternalGettableSettable[A] {
  def path: JsPath
  def cleanup(value: Option[A], userAnswers: UserAnswers): Try[UserAnswers]
}

// Default implementation of GettableSettable
object GettableSettable extends GettableSettable[Nothing] {
  // Assign a meaningful default JsPath (e.g., empty path)
  override val path: JsPath = JsPath \ "defaultPath"

  override def cleanup(value: Option[Nothing], userAnswers: UserAnswers): Try[UserAnswers] =
    Success(userAnswers)
}
