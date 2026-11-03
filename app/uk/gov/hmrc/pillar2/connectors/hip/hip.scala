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

package uk.gov.hmrc.pillar2.connectors

import play.api.Logger
import play.api.http.{HeaderNames as PlayHeaderNames, MimeTypes}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.pillar2.config.AppConfig

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

package object hip {

  private val logger: Logger = Logger("uk.gov.hmrc.pillar2.connectors.hip")

  private[connectors] def hipHeaders(config: AppConfig)(using
    headerCarrier: HeaderCarrier,
    pillar2Id:     String
  ): Seq[(String, String)] = {
    val authHeader = headerCarrier
      .copy(authorization = Some(Authorization(s"Basic ${config.hipKey}")))

    val correlationId = UUID.randomUUID.toString
    logger.info(s"HIP Request correlationId: $correlationId")
    Seq(
      "correlationid"        -> correlationId,
      "X-Originating-System" -> "MDTP",
      "X-Pillar2-Id"         -> pillar2Id,
      "X-Receipt-Date"       -> ZonedDateTime
        .now(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.SECONDS)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
      "X-Transmitting-System" -> "HIP"
    ) ++ authHeader.headers(Seq(HeaderNames.authorisation))
  }

  private[connectors] def hipHeadersCreateRepayment(correlationId: String, appConfig: AppConfig): Seq[(String, String)] =
    Seq(
      PlayHeaderNames.ACCEPT  -> MimeTypes.JSON,
      "correlationid"         -> correlationId,
      "X-Originating-System"  -> "PLR",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${appConfig.hipKey}"
    )

}
