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
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import scala.collection.JavaConverters._

object WireMockHelper {
  this: BaseISpec =>

  def stub(method: MappingBuilder, response: ResponseDefinitionBuilder): StubMapping =
    stubFor(method.willReturn(response))

  def stubGet(uri: String, responseBody: String): StubMapping =
    stub(get(urlEqualTo(uri)), okJson(responseBody))

  def stubPost(url: String, responseStatus: Integer, responseBody: String): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody)
        )
    )
  }

  def stubPut(url: String, status: Integer, response: String, responseHeader: Map[String, String] = Map.empty): StubMapping = {
    val httpHeader = responseHeader.map { case (key, value) =>
      new HttpHeader(key, value)
    }
    val responseHeaders = new HttpHeaders(httpHeader.asJava)
    removeStub(put(urlMatching(url)))
    stubFor(
      put(url)
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(status)
            .withHeaders(responseHeaders)
        )
    )
  }
}
