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
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.connectors.{RegistrationConnector, SubscriptionConnector}
import uk.gov.hmrc.pillar2.controllers.auth.AuthAction
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.pillar2.service.{RegistrationService, SubscriptionService}
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait AllMocks extends MockitoSugar {
  me: BeforeAndAfterEach =>

  val mockActorSystem:                ActorSystem                 = mock[ActorSystem]
  val mockAuditConnector:             AuditConnector              = mock[AuditConnector]
  val mockAuthConnector:              AuthConnector               = mock[AuthConnector]
  val mockAppConfig:                  AppConfig                   = mock[AppConfig]
  val mockRgistrationCacheRepository: RegistrationCacheRepository = mock[RegistrationCacheRepository]
  val mockHttpClient:                 HttpClient                  = mock[HttpClient]
  val mockDataSubmissionsConnector:   RegistrationConnector       = mock[RegistrationConnector]
  val mockDataSubmissionsService:     RegistrationService         = mock[RegistrationService]
  val mockSubscriptionConnector:      SubscriptionConnector       = mock[SubscriptionConnector]
  val mockSubscriptionService:        SubscriptionService         = mock[SubscriptionService]
  val mockCountryOptions:             CountryOptions              = mock[CountryOptions]
  val mockAuditService:               AuditService                = mock[AuditService]
  val mockAuthAction:                 AuthAction                  = mock[AuthAction]
  val mockAuthorisedFunctions:        AuthorisedFunctions         = mock[AuthorisedFunctions]
  override protected def beforeEach(): Unit =
    Seq(
      mockActorSystem,
      mockAuditConnector,
      mockAuthConnector,
      mockAppConfig,
      mockRgistrationCacheRepository,
      mockHttpClient,
      mockDataSubmissionsConnector,
      mockDataSubmissionsService,
      mockSubscriptionConnector,
      mockSubscriptionService,
      mockCountryOptions,
      mockAuditService,
      mockAuthAction,
      mockAuthorisedFunctions
    ).foreach(Mockito.reset(_))
}
