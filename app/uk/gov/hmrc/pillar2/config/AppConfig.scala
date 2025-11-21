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

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  val defaultDataExpireInSeconds: Long = config.get[Long]("defaultDataExpireInSeconds")

  def baseUrl(serviceName: String): String =
    s"${servicesConfig.baseUrl(serviceName)}${servicesConfig.getString(s"microservice.services.$serviceName.context")}"

  val registrationCacheCryptoKey: String  = config.get[String]("registrationCache.key")
  val cryptoToggle:               Boolean = config.get[Boolean]("encryptionToggle")

  val bearerToken:                String => String = (serviceName: String) => config.get[String](s"microservice.services.$serviceName.bearer-token")
  lazy val hipKey:                String           = config.get[String]("hip.key")
  val environment:                String => String = (serviceName: String) => config.get[String](s"microservice.services.$serviceName.environment")
  lazy val locationCanonicalList: String           = config.get[String]("location.canonical.list.all")
}
