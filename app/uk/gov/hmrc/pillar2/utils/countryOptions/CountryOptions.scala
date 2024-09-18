/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.pillar2.utils.countryOptions

import play.api.Environment
import play.api.libs.json.Json
import uk.gov.hmrc.pillar2.config.AppConfig

import javax.inject.{Inject, Singleton}

@Singleton
class CountryOptions @Inject() (environment: Environment, config: AppConfig) {
  def options: Seq[InputOption] = CountryOptions.getCountries(environment, config.locationCanonicalList)

  def getCountryCodeFromName(name: String): String =
    options
      .collectFirst { case InputOption(value, `name`, _) => value }
      .getOrElse(name)
}

object CountryOptions {
  def getCountries(environment: Environment, fileName: String): Seq[InputOption] =
    environment
      .resourceAsStream(fileName)
      .flatMap { in =>
        val locationJsValue = Json.parse(in)
        Json.fromJson[Seq[Seq[String]]](locationJsValue).asOpt.map {
          _.flatMap { countryList =>
            for {
              code <- countryList.lift(1)
              name <- countryList.headOption
            } yield InputOption(code.replaceAll("country:", ""), name)
          }
        }
      }
      .getOrElse {
        play.api.Logger(getClass).error(s"Country JSON file not found or invalid: $fileName")
        Seq.empty[InputOption]
      }
}
