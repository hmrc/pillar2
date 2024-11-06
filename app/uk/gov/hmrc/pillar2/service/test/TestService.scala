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

package uk.gov.hmrc.pillar2.service.test

import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestService @Inject() (
  registrationCacheRepository: RegistrationCacheRepository
)(implicit ec:                 ExecutionContext) {

  def clearAllData: Future[Unit] = {
    val cacheRepositoryDropF = Mdc.preservingMdc(registrationCacheRepository.collection.drop().toFuture()).flatMap { _ =>
      Mdc.preservingMdc(registrationCacheRepository.ensureIndexes())
    }

    for {
      _ <- cacheRepositoryDropF
    } yield ()
  }
}
