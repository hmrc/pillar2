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

import javax.inject.{Inject, Named, Singleton}

@Singleton
class LogUtility @Inject() (
  @Named("platformLogLimit") platformLogLimit: Int
) {
//TODO
  /*  private def isUnderSizeLimit(request: String, limit: Int) = request.getBytes("UTF-8").length < limit

  def getPartialBody(message: String, limit: Int): String =
    message.getBytes("UTF-8").take(limit).map(_.toChar).mkString

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def logInfoWithSizeLimit(message: String, limit: Int = platformLogLimit)(implicit logger: Logger): String =
    if (isUnderSizeLimit(message, limit)) {
      logger.info(message)
      message
    } else {
      val partial = getPartialBody(message, limit)
      logger.info(partial)
      partial
    }*/
}
