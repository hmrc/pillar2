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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  val mockServerHost: String = "localhost"
  val mockServerPort: Int    = 1234
  val mockServerUrl = s"http://$mockServerHost:$mockServerPort"

  val mockServer = new WireMockServer(wireMockConfig().port(mockServerPort))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor("localhost", mockServerPort)
    mockServer.start()
  }

  override protected def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit = {
    WireMock.reset()
    super.afterEach()
  }

  override protected def afterAll(): Unit = {
    mockServer.stop()
    super.afterAll()
  }
}
