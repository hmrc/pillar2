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

import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.pillar2.config.AppConfig
import uk.gov.hmrc.pillar2.connectors.*
import uk.gov.hmrc.pillar2.controllers.actions.AuthAction
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.*
import uk.gov.hmrc.pillar2.service.audit.AuditService
import uk.gov.hmrc.pillar2.utils.countryOptions.CountryOptions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait AllMocks extends MockitoSugar {
  me: BeforeAndAfterEach =>

  val mockActorSystem:                        ActorSystem                        = mock[ActorSystem]
  val mockAuditConnector:                     AuditConnector                     = mock[AuditConnector]
  val mockAuthConnector:                      AuthConnector                      = mock[AuthConnector]
  val mockAppConfig:                          AppConfig                          = mock[AppConfig]
  val mockRegistrationCacheRepository:        RegistrationCacheRepository        = mock[RegistrationCacheRepository]
  val mockHttpClient:                         HttpClientV2                       = mock[HttpClientV2]
  val mockDataSubmissionsConnector:           RegistrationConnector              = mock[RegistrationConnector]
  val mockRepaymentConnector:                 RepaymentConnector                 = mock[RepaymentConnector]
  val mockDataSubmissionsService:             RegistrationService                = mock[RegistrationService]
  val mockSubscriptionConnector:              SubscriptionConnector              = mock[SubscriptionConnector]
  val mockSubscriptionService:                SubscriptionService                = mock[SubscriptionService]
  val mockUKTaxReturnService:                 UKTaxReturnService                 = mock[UKTaxReturnService]
  val mockUKTaxReturnConnector:               UKTaxReturnConnector               = mock[UKTaxReturnConnector]
  val mockRepaymentService:                   RepaymentService                   = mock[RepaymentService]
  val mockCountryOptions:                     CountryOptions                     = mock[CountryOptions]
  val mockAuditService:                       AuditService                       = mock[AuditService]
  val mockAuthAction:                         AuthAction                         = mock[AuthAction]
  val mockAuthorisedFunctions:                AuthorisedFunctions                = mock[AuthorisedFunctions]
  val mockFinancialDataConnector:             FinancialDataConnector             = mock[FinancialDataConnector]
  val mockFinancialService:                   FinancialService                   = mock[FinancialService]
  val mockBTNService:                         BTNService                         = mock[BTNService]
  val mockBTNConnector:                       BTNConnector                       = mock[BTNConnector]
  val mockObligationsAndSubmissionsService:   ObligationsAndSubmissionsService   = mock[ObligationsAndSubmissionsService]
  val mockObligationsAndSubmissionsConnector: ObligationsAndSubmissionsConnector = mock[ObligationsAndSubmissionsConnector]
  val mockOrnService:                         ORNService                         = mock[ORNService]
  val mockOrnConnector:                       ORNConnector                       = mock[ORNConnector]

  override protected def beforeEach(): Unit =
    Seq(
      mockActorSystem,
      mockAuditConnector,
      mockAuthConnector,
      mockRepaymentConnector,
      mockAppConfig,
      mockRegistrationCacheRepository,
      mockHttpClient,
      mockRepaymentService,
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
