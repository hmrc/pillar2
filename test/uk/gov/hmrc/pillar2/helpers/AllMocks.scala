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
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.connectors.DataSubmissionsConnector
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.DataSubmissionsService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait AllMocks extends MockitoSugar {
  me: BeforeAndAfterEach =>

  val mockActorSystem:                ActorSystem                 = mock[ActorSystem]
  val mockAuditConnector:             AuditConnector              = mock[AuditConnector]
  val mockAuthConnector:              AuthConnector               = mock[AuthConnector]
  val mockAppConfig:                  AppConfig                   = mock[AppConfig]
  val mockRgistrationCacheRepository: RegistrationCacheRepository = mock[RegistrationCacheRepository]
  val mockHttpClient:                 HttpClient                  = mock[HttpClient]
  val mockDataSubmissionsConnector:   DataSubmissionsConnector    = mock[DataSubmissionsConnector]
  val mockDataSubmissionsService:     DataSubmissionsService      = mock[DataSubmissionsService]
  val mockAuthAction:                 AuthAction                  = mock[AuthAction]

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
      mockAuthAction
    ).foreach(Mockito.reset(_))
}
