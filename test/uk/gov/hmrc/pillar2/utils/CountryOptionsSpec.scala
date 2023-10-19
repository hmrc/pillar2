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

package uk.gov.hmrc.pillar2.utils

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.utils.countryOptions.{CountryOptions, InputOption}

class CountryOptionsSpec extends BaseSpec {

  "Country Options" - {

    "build correctly the InputOptions with country list and country code" in {
      val app =
        new GuiceApplicationBuilder()
          .configure(
            Map(
              "location.canonical.list.all" -> "country-canonical-list-test.json",
              "metrics.enabled"             -> "false"
            )
          )
          .build()

      running(app) {

        val countryOption: CountryOptions = app.injector.instanceOf[CountryOptions]
        countryOption.options mustEqual Seq(InputOption("AF", "Afghanistan"), InputOption("AG", "Antigua and Barbuda"))
      }
    }
  }
}
