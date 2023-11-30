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

package uk.gov.hmrc.pillar2.helpers

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Inside, Inspectors, LoneElement, OptionValues, Status => _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.{Application, Mode}
import uk.gov.hmrc.pillar2.{FakeObjects, ResultAssertions}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal



abstract class BaseISpec
    extends AnyWordSpec
    with CleanMongo
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with Inspectors
    with ScalaFutures
    with DefaultAwaitTimeout
    with Writeables
    with FutureAwaits
    with EssentialActionCaller
    with RouteInvokers
    with LoneElement
    with Inside
    with OptionValues
    with Results
    with Status
    with HeaderNames
    with MimeTypes
    with HttpProtocol
    with HttpVerbs
    with ResultExtractors
    with ResultAssertions
    with AdditionalAppConfig
    with FakeObjects {

  implicit lazy val system:       ActorSystem      = ActorSystem()
  implicit lazy val materializer: Materializer     = Materializer(system)
  implicit def ec:                ExecutionContext = global

  additionalAppConfig ++= Map(
    "mongodb.uri"      -> "mongodb://localhost:27017/pillar2-test",
    "metrics.enabled"  -> false,
    "auditing.enabled" -> false
  )

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Millis))

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(additionalAppConfig.toMap)
      .in(Mode.Test)
      .build()

  val contentType: (String, String) = "Content-Type" -> "application/json"

  def fakeRequest(call: Call): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(call).withHeaders(uk.gov.hmrc.http.HeaderNames.authorisation -> "some bearer token")


  def callRoute[A](req: Request[A])(implicit app: Application, w: Writeable[A]): Future[Result] = {
    val errorHandler = app.errorHandler

    route(app, req) match {
      case None => fail("Route does not exist")
      case Some(fResult) =>
        fResult.recoverWith {
          case NonFatal(t) => errorHandler.onServerError(req, t)
        }
    }
  }
}
