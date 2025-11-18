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

package uk.gov.hmrc.pillar2.controllers

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.helpers.wiremock.WireMockServerHandler
import uk.gov.hmrc.pillar2.helpers.{AuthStubs, CleanMongo}
import uk.gov.hmrc.pillar2.repositories.{RegistrationCacheRepository, RegistrationDataEntry}

import java.net.URI
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationCacheControllerISpec
    extends AnyFunSuite
    with GuiceOneServerPerSuite
    with WireMockServerHandler
    with AuthStubs
    with OptionValues
    with CleanMongo {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.auth.port" -> wiremockPort)
    .configure("metrics.enabled" -> false)
    .build()

  given headerCarrier: HeaderCarrier =
    HeaderCarrier(authorization = Option(Authorization("bearerToken"))).withExtraHeaders("Content-Type" -> "application/json")

  private val userAnswersCache =
    RegistrationDataEntry(
      "id",
      Json.toJson(("foo" -> "bar", "name" -> "steve", "address" -> "address1")).toString(),
      Instant.now()
    )

  test("Save data successfully") {
    val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]
    stubAuthenticate()
    val example    = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
    val httpClient = app.injector.instanceOf[HttpClientV2]
    val url        = routes.RegistrationCacheController.save(userAnswersCache.id).url
    val request    = httpClient.post(URI.create(s"http://localhost:$port$url").toURL).withBody(example)
    val result     = await(request.execute[HttpResponse])
    result.status shouldBe 200
    val expectedResult = registrationCacheRepository.get(userAnswersCache.id).futureValue.value
    example shouldBe expectedResult
  }

  test("Get registration cache") {
    val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]
    stubAuthenticate()
    val example    = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
    await(registrationCacheRepository.upsert(userAnswersCache.id, example))
    val httpClient = app.injector.instanceOf[HttpClientV2]
    val url        = routes.RegistrationCacheController.get(userAnswersCache.id)
    val request    = httpClient.get(URI.create(s"http://localhost:$port$url").toURL).withBody(example)
    val result     = await(request.execute[HttpResponse])
    result.status shouldBe 200
    result.json shouldEqual example
  }

  test("Remove item from registration cache") {
    val registrationCacheRepository: RegistrationCacheRepository = app.injector.instanceOf[RegistrationCacheRepository]
    stubAuthenticate()
    val example    = Json.parse(getClass.getResourceAsStream("/data/userAnswers_request.json"))
    await(registrationCacheRepository.upsert(userAnswersCache.id, example))
    val httpClient = app.injector.instanceOf[HttpClientV2]
    val url        = routes.RegistrationCacheController.remove(userAnswersCache.id)
    val request    = httpClient.delete(URI.create(s"http://localhost:$port$url").toURL)
    val result     = await(request.execute[HttpResponse])
    result.status shouldBe 200
     registrationCacheRepository.get(userAnswersCache.id).futureValue.isEmpty shouldBe true
  }

}
