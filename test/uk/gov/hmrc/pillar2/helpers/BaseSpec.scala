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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice._
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.utils.LogUtility

import scala.concurrent.ExecutionContext

trait BaseSpec
    extends AnyFreeSpec
    with Matchers
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with AllMocks
    with ScalaFutures
    with OptionValues
    with Configs
    with Status
    with WireMockServerHandler
    with UKTaxReturnDataFixture {

  implicit lazy val ec:           ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc:           HeaderCarrier    = HeaderCarrier()
  implicit lazy val system:       ActorSystem      = ActorSystem()
  implicit lazy val materializer: Materializer     = Materializer(system)
  val contentType:                (String, String) = "Content-Type" -> "application/json"

  def injector: Injector = app.injector

  def frontendAppConfig: AppConfig = injector.instanceOf[AppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(contentType)

  val PLATFORM_LOG_LIMIT = 12288

  val logUtils = new LogUtility

  protected def applicationBuilder(): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
      )
      .overrides()

  protected def stubResponse(
    expectedUrl:    String,
    expectedStatus: Int
  ): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )

  protected def stubGetResponse(
    expectedUrl:    String,
    expectedStatus: Int
  ): StubMapping =
    server.stubFor(
      get(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )

  protected def stubPutResponse(
    expectedUrl:    String,
    expectedStatus: Int
  ): StubMapping =
    server.stubFor(
      put(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )

  protected def verifyHipHeaders(method: String, etmpUrl: String, expectedBody: Option[String] = None): Unit = {
    val requestBuilder = method match {
      case "GET"  => getRequestedFor(urlEqualTo(etmpUrl))
      case "POST" => postRequestedFor(urlEqualTo(etmpUrl))
      case "PUT"  => putRequestedFor(urlEqualTo(etmpUrl))
    }

    val getVerification = requestBuilder
      .withHeader("correlationid", matching("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
      .withHeader("X-Originating-System", equalTo("MDTP"))
      .withHeader("X-Pillar2-Id", equalTo(pillar2Id))
      .withHeader("X-Receipt-Date", matching("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"))
      .withHeader("X-Transmitting-System", equalTo("HIP"))
      .withHeader("Authorization", equalTo("Basic hip-key"))

    val ifHasBody = expectedBody.fold(getVerification)(body => getVerification.withRequestBody(equalToJson(body)))

    server.verify(ifHasBody)
  }

}
