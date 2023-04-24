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

package uk.gov.hmrc.pillar2.config

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit =
    bind(classOf[AppConfig]).asEagerSingleton()

  /*  @Provides
  @Named("eisBearerToken")
  @Singleton
  def eisBearerTokenProvider(servicesConfig: ServicesConfig): String =
    servicesConfig.getString("microservice.services.eis.bearer-token")

  @Provides
  @Named("eisUrl")
  @Singleton
  def eisUrlProvider(servicesConfig: ServicesConfig): String =
    servicesConfig.baseUrl("eis")

  @Provides
  @Named("eisEnvironment")
  @Singleton
  def eisEnvironmentProvider(servicesConfig: ServicesConfig): String =
    servicesConfig.getString("microservice.services.eis.environment")

  @Provides
  @Named("platformLogLimit")
  @Singleton
  def platformLogLimit =
    configuration.get[Int]("mdtp.log.size.max")

  @Provides
  @Named("platformAnalyticsUrl")
  @Singleton
  def platformAnalyticsUrlProvider(servicesConfig: ServicesConfig): String =
    servicesConfig.baseUrl("platform-analytics")

  @Provides
  @Named("platformAnalyticsTrackingId")
  @Singleton
  protected def platformAnalyticsTrackingIdProvider(configuration: Configuration): String =
    configuration.get[String](s"google-analytics.trackingId")*/

}
